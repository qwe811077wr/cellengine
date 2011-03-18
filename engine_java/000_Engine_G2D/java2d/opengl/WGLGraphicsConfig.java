/*
 * %W% %E%
 *
 * Copyright (c) 2006, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package sun.java2d.opengl;

import java.awt.AWTException;
import java.awt.BufferCapabilities;
import java.awt.BufferCapabilities.FlipContents;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.ImageCapabilities;
import java.awt.Rectangle;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DirectColorModel;
import java.awt.image.VolatileImage;
import sun.awt.Win32GraphicsConfig;
import sun.awt.Win32GraphicsDevice;
import sun.awt.image.SunVolatileImage;
import sun.awt.image.SurfaceManager;
import sun.awt.windows.WComponentPeer;
import sun.java2d.Disposer;
import sun.java2d.DisposerRecord;
import sun.java2d.SunGraphics2D;
import sun.java2d.Surface;
import sun.java2d.SurfaceData;
import sun.java2d.pipe.hw.AccelSurface;
import sun.java2d.pipe.hw.AccelTypedVolatileImage;
import sun.java2d.pipe.hw.ContextCapabilities;
import static sun.java2d.opengl.OGLSurfaceData.*;
import static sun.java2d.opengl.OGLContext.OGLContextCaps.*;
import static sun.java2d.opengl.WGLSurfaceData.*;
import sun.java2d.opengl.OGLContext.OGLContextCaps;
import sun.java2d.pipe.hw.AccelDeviceEventListener;
import sun.java2d.pipe.hw.AccelDeviceEventNotifier;
import sun.java2d.windows.GDIWindowSurfaceData;

public class WGLGraphicsConfig
    extends Win32GraphicsConfig
    implements OGLGraphicsConfig
{
    protected static boolean wglAvailable;
    private static ImageCapabilities imageCaps = new WGLImageCaps();

    private BufferCapabilities bufferCaps;
    private long pConfigInfo;
    private ContextCapabilities oglCaps;
    private OGLContext context;
    private Object disposerReferent = new Object();

    public static native int getDefaultPixFmt(int screennum);
    private static native boolean initWGL();
    private static native long getWGLConfigInfo(int screennum, int visualnum);
    private static native int getOGLCapabilities(long configInfo);

    static {
        wglAvailable = initWGL();
    }

    protected WGLGraphicsConfig(Win32GraphicsDevice device, int visualnum,
                                long configInfo, ContextCapabilities oglCaps)
    {
        super(device, visualnum);
        this.pConfigInfo = configInfo;
        this.oglCaps = oglCaps;
        context = new OGLContext(OGLRenderQueue.getInstance(), this);

        // add a record to the Disposer so that we destroy the native
        // WGLGraphicsConfigInfo data when this object goes away
        Disposer.addRecord(disposerReferent,
                           new WGLGCDisposerRecord(pConfigInfo,
                                                   device.getScreen()));
    }

    public static WGLGraphicsConfig getConfig(Win32GraphicsDevice device,
                                              int pixfmt)
    {
        if (!wglAvailable) {
            return null;
        }

        long cfginfo = 0;
        final String ids[] = new String[1];
        OGLRenderQueue rq = OGLRenderQueue.getInstance();
        rq.lock();
        try {
            // getWGLConfigInfo() creates and destroys temporary
            // surfaces/contexts, so we should first invalidate the current
            // Java-level context and flush the queue...
            OGLContext.invalidateCurrentContext();
            WGLGetConfigInfo action =
                new WGLGetConfigInfo(device.getScreen(), pixfmt);
            rq.flushAndInvokeNow(action);
            cfginfo = action.getConfigInfo();
            if (cfginfo != 0L) {
                OGLContext.setScratchSurface(cfginfo);
                rq.flushAndInvokeNow(new Runnable() {
                    public void run() {
                        ids[0] = OGLContext.getOGLIdString();
                    }
                });
            }
        } finally {
            rq.unlock();
        }
        if (cfginfo == 0) {
            return null;
        }

        int oglCaps = getOGLCapabilities(cfginfo);
        ContextCapabilities caps = new OGLContextCaps(oglCaps, ids[0]);

        return new WGLGraphicsConfig(device, pixfmt, cfginfo, caps);
    }

    /**
     * This is a small helper class that allows us to execute
     * getWGLConfigInfo() on the queue flushing thread.
     */
    private static class WGLGetConfigInfo implements Runnable {
        private int screen;
        private int pixfmt;
        private long cfginfo;
        private WGLGetConfigInfo(int screen, int pixfmt) {
            this.screen = screen;
            this.pixfmt = pixfmt;
        }
        public void run() {
            cfginfo = getWGLConfigInfo(screen, pixfmt);
        }
        public long getConfigInfo() {
            return cfginfo;
        }
    }

    public static boolean isWGLAvailable() {
        return wglAvailable;
    }

    /**
     * Returns true if the provided capability bit is present for this config.
     * See OGLContext.java for a list of supported capabilities.
     */
    public final boolean isCapPresent(int cap) {
        return ((oglCaps.getCaps() & cap) != 0);
    }

    public final long getNativeConfigInfo() {
        return pConfigInfo;
    }

    /**
     * {@inheritDoc}
     *
     * @see sun.java2d.pipe.hw.BufferedContextProvider#getContext
     */
    public final OGLContext getContext() {
        return context;
    }

    private static class WGLGCDisposerRecord implements DisposerRecord {
        private long pCfgInfo;
        private int screen;
        public WGLGCDisposerRecord(long pCfgInfo, int screen) {
            this.pCfgInfo = pCfgInfo;
        }
        public void dispose() {
            OGLRenderQueue rq = OGLRenderQueue.getInstance();
            rq.lock();
            try {
                rq.flushAndInvokeNow(new Runnable() {
                    public void run() {
                        AccelDeviceEventNotifier.
                            eventOccured(screen,
                                AccelDeviceEventNotifier.DEVICE_RESET);
                        AccelDeviceEventNotifier.
                            eventOccured(screen,
                                AccelDeviceEventNotifier.DEVICE_DISPOSED);
                    }
                });
            } finally {
                rq.unlock();
            }
            if (pCfgInfo != 0) {
                OGLRenderQueue.disposeGraphicsConfig(pCfgInfo);
                pCfgInfo = 0;
            }
        }
    }

    @Override
    public synchronized void displayChanged() {
        super.displayChanged();
        // the context could hold a reference to a WGLSurfaceData, which in
        // turn has a reference back to this WGLGraphicsConfig, so in order
        // for this instance to be disposed we need to break the connection
        OGLRenderQueue rq = OGLRenderQueue.getInstance();
        rq.lock();
        try {
            OGLContext.invalidateCurrentContext();
        } finally {
            rq.unlock();
        }
    }

    @Override
    public ColorModel getColorModel(int transparency) {
	switch (transparency) {
	case Transparency.OPAQUE:
            // REMIND: once the ColorModel spec is changed, this should be
            //         an opaque premultiplied DCM...
            return new DirectColorModel(24, 0xff0000, 0xff00, 0xff);
	case Transparency.BITMASK:
            return new DirectColorModel(25, 0xff0000, 0xff00, 0xff, 0x1000000);
	case Transparency.TRANSLUCENT:
            ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_sRGB);
            return new DirectColorModel(cs, 32,
                                        0xff0000, 0xff00, 0xff, 0xff000000,
                                        true, DataBuffer.TYPE_INT);
	default:
	    return null;
        }
    }

    @Override
    public String toString() {
	return ("WGLGraphicsConfig[dev="+screen+",pixfmt="+visual+"]");
    }

    /**
     * The following methods are invoked from WComponentPeer.java rather
     * than having the Win32-dependent implementations hardcoded in that
     * class.  This way the appropriate actions are taken based on the peer's
     * GraphicsConfig, whether it is a Win32GraphicsConfig or a
     * WGLGraphicsConfig.
     */

    /**
     * Creates a new SurfaceData that will be associated with the given
     * WComponentPeer.
     */
    @Override
    public SurfaceData createSurfaceData(WComponentPeer peer,
                                         int numBackBuffers)
    {
        SurfaceData sd = WGLSurfaceData.createData(peer);
        if (sd == null) {
            sd = GDIWindowSurfaceData.createData(peer);
        }
        return sd;
    }

    /**
     * The following methods correspond to the multibuffering methods in
     * WComponentPeer.java...
     */

    /**
     * Checks that the requested configuration is natively supported; if not,
     * an AWTException is thrown.
     */
    @Override
    public void assertOperationSupported(Component target,
                                         int numBuffers,
                                         BufferCapabilities caps)
        throws AWTException
    {
        if (numBuffers > 2) {
            throw new AWTException(
                "Only double or single buffering is supported");
        }
        BufferCapabilities configCaps = getBufferCapabilities();
        if (!configCaps.isPageFlipping()) {
            throw new AWTException("Page flipping is not supported");
        }
        if (caps.getFlipContents() == BufferCapabilities.FlipContents.PRIOR) {
            throw new AWTException("FlipContents.PRIOR is not supported");
        }
    }

    /**
     * Creates a WGL-based backbuffer for the given peer and returns the
     * image wrapper.
     */
    @Override
    public VolatileImage createBackBuffer(WComponentPeer peer) {
        Component target = (Component)peer.getTarget();
        return new SunVolatileImage(target,
                                    target.getWidth(), target.getHeight(),
                                    Boolean.TRUE);
    }

    /**
     * Performs the native WGL flip operation for the given target Component.
     */
    @Override
    public void flip(WComponentPeer peer,
                     Component target, VolatileImage backBuffer,
                     int x1, int y1, int x2, int y2,
                     BufferCapabilities.FlipContents flipAction)
    {
        if (flipAction == BufferCapabilities.FlipContents.COPIED) {
            SurfaceManager vsm = SurfaceManager.getManager(backBuffer);
            SurfaceData sd = vsm.getDestSurfaceData();

            if (sd instanceof WGLVSyncOffScreenSurfaceData) {
                WGLVSyncOffScreenSurfaceData vsd =
                    (WGLVSyncOffScreenSurfaceData)sd;
                SurfaceData bbsd = vsd.getFlipSurface();
                Graphics2D bbg =
                    new SunGraphics2D(bbsd, Color.black, Color.white, null);
                try {
                    bbg.drawImage(backBuffer, 0, 0, null);
                } finally {
                    bbg.dispose();
                }
            } else {
                Graphics g = peer.getGraphics();
                try {
                    g.drawImage(backBuffer,
                                x1, y1, x2, y2,
                                x1, y1, x2, y2,
                                null);
                } finally {
                    g.dispose();
                }
                return;
            }
        } else if (flipAction == BufferCapabilities.FlipContents.PRIOR) {
            // not supported by WGL...
            return;
        }

        OGLSurfaceData.swapBuffers(peer.getData());

        if (flipAction == BufferCapabilities.FlipContents.BACKGROUND) {
            Graphics g = backBuffer.getGraphics();
            try {
                g.setColor(target.getBackground());
                g.fillRect(0, 0,
                           backBuffer.getWidth(),
                           backBuffer.getHeight());
            } finally {
                g.dispose();
            }
        }
    }

    private static class WGLBufferCaps extends BufferCapabilities {
        public WGLBufferCaps(boolean dblBuf) {
            super(imageCaps, imageCaps,
                  dblBuf ? FlipContents.UNDEFINED : null);
        }
    }

    @Override
    public BufferCapabilities getBufferCapabilities() {
        if (bufferCaps == null) {
            boolean dblBuf = isCapPresent(CAPS_DOUBLEBUFFERED);
            bufferCaps = new WGLBufferCaps(dblBuf);
        }
        return bufferCaps;
    }

    private static class WGLImageCaps extends ImageCapabilities {
        private WGLImageCaps() {
            super(true);
        }
        public boolean isTrueVolatile() {
            return true;
        }
    }

    @Override
    public ImageCapabilities getImageCapabilities() {
        return imageCaps;
    }

    /**
     * {@inheritDoc}
     *
     * @see sun.java2d.pipe.hw.AccelGraphicsConfig#createCompatibleVolatileImage
     */
    public VolatileImage
        createCompatibleVolatileImage(int width, int height,
                                      int transparency, int type)
    {
        if (type == FLIP_BACKBUFFER || type == WINDOW || type == UNDEFINED ||
            transparency == Transparency.BITMASK)
        {
            return null;
        }

        if (type == FBOBJECT) {
            if (!isCapPresent(CAPS_EXT_FBOBJECT)) {
                return null;
            }
        } else if (type == PBUFFER) {
            boolean isOpaque = transparency == Transparency.OPAQUE;
            if (!isOpaque && !isCapPresent(CAPS_STORED_ALPHA)) {
                return null;
            }
        }

        SunVolatileImage vi = new AccelTypedVolatileImage(this, width, height,
                                                          transparency, type);
        Surface sd = vi.getDestSurface();
        if (!(sd instanceof AccelSurface) ||
            ((AccelSurface)sd).getType() != type)
        {
            vi.flush();
            vi = null;
        }

        return vi;
    }

    /**
     * {@inheritDoc}
     *
     * @see sun.java2d.pipe.hw.AccelGraphicsConfig#getContextCapabilities
     */
    public ContextCapabilities getContextCapabilities() {
        return oglCaps;
    }

    public void addDeviceEventListener(AccelDeviceEventListener l) {
        AccelDeviceEventNotifier.addListener(l, screen.getScreen());
    }

    public void removeDeviceEventListener(AccelDeviceEventListener l) {
        AccelDeviceEventNotifier.removeListener(l);
    }
}