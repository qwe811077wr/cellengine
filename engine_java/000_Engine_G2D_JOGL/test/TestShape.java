
import com.cell.CUtil;
import com.g2d.BufferedImage;
import com.g2d.Color;
import com.g2d.Engine;
import com.g2d.Graphics2D;
import com.g2d.Tools;
import com.g2d.display.DisplayObject;
import com.g2d.display.DisplayObjectContainer;
import com.g2d.display.Sprite;
import com.g2d.display.event.MouseEvent;


public class TestShape extends Sprite
{
	BufferedImage image	= Tools.readImage("/image.png");

	BufferedImage cp 	= Tools.readImage("/copper.png");
	
	public TestShape() 
	{
//		rotate	= Math.random();
//		scale_x	= Math.random() / 1f;
//		scale_x	= Math.random() / 1f;
		
		local_bounds.setBounds(
				-image.getWidth()/2,
				-image.getHeight()/2, 
				image.getWidth(), 
				image.getHeight());
		
		addChild(new Copper(-50, 0));
		addChild(new Copper( 50, 0));
		addChild(new Copper( 0, -100));
		addChild(new Copper( 0, 100));
		
		
	}
	
	@Override
	public void removed(DisplayObjectContainer parent) {
		super.removed(parent);
//		this.image.dispose();
//		this.cp.dispose();
	}
	
	@Override
	public void update()
	{
		if (getRoot().isMouseHold(MouseEvent.BUTTON_LEFT)) 
		{	
			double offset_x = CUtil.getRandom(-100, 100);
			double offset_y = CUtil.getRandom(-100, 100);
			setLocation(
					getParent().getMouseX() + offset_x,
					getParent().getMouseY() + offset_y);
		}
		
		if (getRoot().isMouseHold(MouseEvent.BUTTON_RIGHT)) 
		{
			if (getRoot().isMouseWheelDown()) {
				rotate += 0.1f;
			}
			if (getRoot().isMouseWheelUP()) {
				rotate -= 0.1f;
			}
		}
		else 
		{
			if (getRoot().isMouseWheelDown()) {
				scale_x += 0.1f;
				scale_y += 0.1f;
			}
			if (getRoot().isMouseWheelUP()) {
				scale_x -= 0.1f;
				scale_y -= 0.1f;
			}
		}	
		rotate += 0.1f;
	}
	
	@Override
	public void render(Graphics2D g) 
	{
//		g.setAlpha(0.75f);
//		g.clipRect(
//				local_bounds.x-100, 
//				local_bounds.y, 
//				local_bounds.width+200,
//				local_bounds.height);
//		
//		g.pushBlendMode();
//		g.setBlendMode(3, 0.75f);
//		g.setColor(Color.GRAY);
//		g.fillRect(
//				local_bounds.x-100, 
//				local_bounds.y, 
//				local_bounds.width+200,
//				local_bounds.height);
//		
		g.drawImage(image, local_bounds.x, local_bounds.y);
//		g.popBlendMode();
//		
//		g.setColor(Color.GREEN);
//		g.fillRect(0, 0, 32, 32);
//		g.setColor(Color.YELLOW);
//		g.draw(local_bounds);
	}
	
	public class Copper extends Sprite 
	{
		public Copper(int x, int y) {			
			setLocation(x, y);
			local_bounds.setBounds(
					-cp.getWidth()/2,
					-cp.getHeight()/2, 
					cp.getWidth(), 
					cp.getHeight());
		}
		
		@Override
		public void render(Graphics2D g) {
			g.drawImage(cp, local_bounds.x, local_bounds.y);
//			System.out.println(g.getClipX() + "," + g.getClipY() + "," + g.getClipWidth() + "," + g.getClipHeight()) ;
		}
		
	}
}
