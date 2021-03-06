package com.net.client.minaimpl
{	
	import com.cell.net.io.MessageFactory;
	import com.cell.net.io.MutualMessage;
	import com.cell.net.io.NetDataInput;
	import com.cell.net.io.NetDataOutput;
	import com.cell.net.io.BinNetDataInput;
	import com.cell.net.io.BinNetDataOutput;
	import com.cell.util.Util;
	import com.net.client.Protocol;
	import com.net.client.ProtocolType;
	import com.net.client.ServerSession;
	import com.net.client.ServerSessionListener;
	
	import flash.errors.*;
	import flash.events.*;
	import flash.net.*;
	import flash.utils.ByteArray;
	import flash.utils.Dictionary;
	import flash.utils.IDataInput;
	
	public class ServerSessionImpl implements com.net.client.ServerSession
	{
		/** 当前远程地址*/
		private var 	serveraddr 			: String; 
		
		/** SOCKET套接字*/
		private var 	connector 			: Socket;
		
		/** 服务器监听器*/
		private var 	listener 			: ServerSessionListener;
		
		/** 消息类型管理器*/
		private var 	message_factory		: MessageFactory;
		
		/** 未解析完的数据*/
		private var 	undecoded_buffer	: ByteArray;
		
		/** 未解析完的包*/
		private var 	uncomplete_package	: ProtocolImpl;
		
//		----------------------------------------------------------------------------

		/** 消息头*/
		private var		protocol_start		: int;
		/** 心跳请求头*/
		private var		heart_beat_req 		: int;
		/** 心跳回馈头*/
		private var		heart_beat_rep 		: int;
		
		/** 消息头固定尺寸*/
		private const	protocol_fixed_size	: int = 4 + 4;
		
//		----------------------------------------------------------------------------
		
		
		
		public function ServerSessionImpl(factory : MessageFactory,  
									  header : int = 0x02000006, 
									  hb_req : int = 0x02000001,
									  hb_rep : int = 0x02000002) 
		{
			this.message_factory = factory;
			this.connector = new Socket();
			this.connector.addEventListener(Event.CLOSE, 						closeHandler);
			this.connector.addEventListener(Event.CONNECT, 						connectHandler);
			this.connector.addEventListener(IOErrorEvent.IO_ERROR, 				ioErrorHandler);
			this.connector.addEventListener(SecurityErrorEvent.SECURITY_ERROR,	securityErrorHandler);
			this.connector.addEventListener(ProgressEvent.SOCKET_DATA, 			socketDataHandler);
			
//			close
//			在服务器关闭套接字连接时调度。	Socket
//			
//			connect
//			在建立网络连接后调度。	Socket
//			
//			ioError
//			在出现输入/输出错误并导致发送或加载操作失败时调度。	Socket
//			
//			outputProgress
//			当套接字将其写缓冲区的数据移到网络传输层时调度	Socket
//			
//			securityError
//			若对 Socket.connect() 的调用尝试连接到调用方的安全沙箱禁止的服务器或低于 1024 的端口，且不存在允许进行此类连接的套接字策略文件，则进行调度。	Socket
//			
//			socketData
//			在套接字接收到数据后调度。
			
			this.protocol_start = header;
			this.heart_beat_req = hb_req;
			this.heart_beat_rep = hb_rep;

		}
		
		public function getMessageFactory() : MessageFactory 
		{
			return message_factory;
		}
		
		public function connect(
			host 		: String, 
			port 		: int, 
			timeout		: int,
			listener 	: ServerSessionListener) : Boolean
		{
			this.serveraddr = host + ":" + port;	
			trace("connecting : " + serveraddr);	
			this.listener = listener;
			if (!connector.connected) {
				this.connector.timeout = timeout;
				this.connector.connect(host, port);
			} else {
				this.listener.connected(this);
			}
			return this.connector.connected;
		}
		
		
		public function getRemoteAddress() : String
		{
			return this.serveraddr;
		}
		
		public function toString() : String 
		{
			return this.connector.toString() + "," + this.serveraddr;
		}
		
		public function isConnected() : Boolean
		{
			return this.connector.connected;
		}
		
		public function disconnect() : void
		{
			if (this.connector.connected) {
				this.connector.close();
				this.listener.disconnected(this, "user close");
			}
		}
		
		public function send(message: MutualMessage) : Boolean
		{
			if (this.connector.connected) {
				var protocol : ProtocolImpl = new ProtocolImpl(protocol_fixed_size);
				protocol.setMessage(message);
				protocol.setPacketNumber(0);
				sendImpl(protocol);
				return true;
			} else {
				return false;
			}
		}
		
		public function sendRequest(pnum: int, message : MutualMessage) : Boolean
		{
			if (this.connector.connected) {
				var protocol : ProtocolImpl = new ProtocolImpl(protocol_fixed_size);
				protocol.setMessage(message);
				protocol.setPacketNumber(pnum);
				sendImpl(protocol);
				return true;
			} else {
				return false;
			}	
		}
		
		private function sendImpl(protocol : ProtocolImpl) : void
		{
//			protocol.setSessionID(0,0);
			protocol.setProtocol(ProtocolType.PROTOCOL_SESSION_MESSAGE);
			protocol.setChannelID(null);
//			protocol.setChannelSessionID(0,0);
			var stream  : ByteArray = encode(protocol);
			if (stream != null) {
				stream.position = 0;
				connector.writeBytes(stream, 0, stream.length);
				connector.flush();
//				trace("Send : " + protocol.getMessage() + " : HexDump : " + Util.dumpHex(stream));
			}
		}
		
//		---------------------------------------------------------------------------------------------------------

		public function getSentMessageCount():int {
			return 0;
		}
		
		public function getReceivedMessageCount () :int {
			return 0;
		}
		
		public function getSentBytes():int {
			return 0;
		}
		
		public function getReceivedBytes():int {
			return 0;
		}
		
		public function getHeartBeatSent():int {
			return 0;
		}
		
		public function getHeartBeatReceived():int {
			return 0;
		}
		
//		---------------------------------------------------------------------------------------------------------
		
		
		private function closeHandler(event:Event):void {
//			trace("closeHandler: " + event);
			this.listener.disconnected(this, event.toString());
		}
		
		private function connectHandler(event:Event):void {
//			trace("connectHandler: " + event);
			this.listener.connected(this);
		}
		
		private function ioErrorHandler(event:IOErrorEvent):void {
//			trace("ioErrorHandler: " + event);
			if (this.connector.connected) {
				this.connector.close();
			}
			this.listener.disconnected(this, event.toString());
//			this.listener.disconnected(this, false, event.toString());
		}
		
		private function securityErrorHandler(event:SecurityErrorEvent):void {
//			trace("securityErrorHandler: " + event);
			if (this.connector.connected) {
				this.connector.close();
			}
			this.listener.disconnected(this, event.toString());
//			this.listener.disconnected(this, false, event.toString());
		}
		
		private function socketDataHandler(event:ProgressEvent):void 
		{
			//trace("socketDataHandler: " + event);
			try
			{
				// 先将socket中的数据读入到 ByteArray
				var avaliable : int = this.connector.bytesAvailable;
				var buf : ByteArray = new ByteArray();
				this.connector.readBytes(buf, 0, avaliable);

				// 如果有未解析完的数据，则将新数据插入到后面
				if (this.undecoded_buffer!=null) {
					buf.position = 0;
					this.undecoded_buffer.writeBytes(buf, 0, buf.length);
					buf = undecoded_buffer;
				}
				
				buf.position = 0;
				
				while (true) 
				{
					// 记录当前位置
					var oldPos : int = buf.position;
					
					// 试图解析一个包
					var decoded : Boolean = decode(buf);
					
					// 如果有数据被解析
					if (decoded)
					{
						// 如果没有数据被读取
						if (buf.position == oldPos){
							try{
								throw new Error("decode() can't return true when buffer is not consumed.");
							}catch(err:Error){
								trace(err + "\n" + err.getStackTrace());
							}
						}
						
						// 若流中无数据，则跳出
						if (!buf.bytesAvailable>0) {
							break;
						}
						
					} else {
						// 若未成功解析，则跳出，等待下一次解析
						break;
					}
				}
				
				if (buf.bytesAvailable>0) {
					// 把未解析完的数据存入状态
					this.undecoded_buffer = new ByteArray();
					this.undecoded_buffer.writeBytes(buf, buf.position, buf.bytesAvailable);
				} else {
					// 如果无数据可以解析，则清空状态
					this.undecoded_buffer = null;
				}
			}
			catch(err:EOFError) 
			{
				trace("socketDataHandler error  : " + err);  		
			}
		}
		
		
//		---------------------------------------------------------------------------------------------------------
		
//		-----------------------------------------------------------------------------------------

		/**
		 * 如果有数据被解析，返回true
		 */
		protected function decode(buffer : ByteArray) : Boolean
		{
			
			//得到上次的状态
			var protocol : ProtocolImpl = this.uncomplete_package;
			
			try
			{
				// 如果上次无状态，则生成新的状态
				if (protocol == null)
				{
					// 有足够的数据
					if (buffer.bytesAvailable >= protocol_fixed_size)
					{
						// 判断是否是有效的数据包头
						var head : int = buffer.readInt();				// 4
						if (head != protocol_start) {
							// 心跳包头，重置。
							if (head == heart_beat_req) {
								return true;
							}
							if (head == heart_beat_rep) {
								return true;
							}
							// 丢弃掉非法字节//返回true代表这次解包已完成,清空状态并准备下一次解包
							trace("bad head, drop data : " + head.toString(16));
							return true;
						}
						
						// 生成新的状态
						protocol = new ProtocolImpl(buffer.readInt());	// 4
						this.uncomplete_package = protocol;
					}
					else
					{
						// 没有足够的数据时,返回 false
						// 返回false代表这次解包未完成,需要等待
						return false;
					}
					
				}
				
				// 继续解析包内容
				if (protocol != null)
				{
					var message_size : int = protocol.buffer_size - protocol_fixed_size;
					
					// 如果有足够的数据
					if (buffer.bytesAvailable >= message_size) 
					{
						// 清空当前的解包状态
						this.uncomplete_package 	= null;
						
						protocol.setReceivedTime			(new Date());
						
						protocol.setProtocol				(buffer.readByte());	// 1
						protocol.setPacketNumber			(buffer.readInt());		// 4
												
						switch (protocol.getProtocol()) {
						case ProtocolType.PROTOCOL_CHANNEL_JOIN_S2C:
						case ProtocolType.PROTOCOL_CHANNEL_LEAVE_S2C:
						case ProtocolType.PROTOCOL_CHANNEL_MESSAGE:
							protocol.setChannelID			(buffer.readInt());		// 4
							break;
						case ProtocolType.PROTOCOL_SYSTEM_NOTIFY:
							protocol.setSystemMessage		(buffer.readInt());		// 4
							break;
						}
						
						var transmission_flag : int	= buffer.readByte();			// 1
						
						if (transmission_flag != 0) 
						{
							// 确定是否要解压缩
							if ((transmission_flag & ProtocolImpl.TRANSMISSION_TYPE_COMPRESSING) != 0) {
								trace("not supprot TRANSMISSION_TYPE_COMPRESSING");
							}
							// 解出包包含的二进制消息
							if ((transmission_flag & ProtocolImpl.TRANSMISSION_TYPE_MUTUAL) != 0) {
								var message : MutualMessage = message_factory.createMessage(buffer.readInt());// ext 4
								var input : BinNetDataInput = new BinNetDataInput(message_factory, buffer);
								message_factory.readExternal(message, input);
								protocol.setMessage(message);
							}
							else if ((transmission_flag & ProtocolImpl.TRANSMISSION_TYPE_EXTERNALIZABLE) != 0) {
								trace("not supprot TRANSMISSION_TYPE_EXTERNALIZABLE");
							}
							else if ((transmission_flag & ProtocolImpl.TRANSMISSION_TYPE_SERIALIZABLE) != 0) {
								trace("not supprot TRANSMISSION_TYPE_SERIALIZABLE");
							}
						}
						
						
						// 告诉 Protocol Handler 有消息被接收到
						recivedMessage(protocol);
						
						//System.out.println("decoded <- " + session.getRemoteAddress() + " : " + protocol);
						
						//ReceivedMessageCount ++;
						
						// 无论如何都返回true，因为当前解包已完成
						return true;
					} 
					else
					{
						// 没有足够的数据时,返回 false
						// 返回false代表这次解包未完成,需要等待
						return false;
					}
				}
				
				return false;
			}
			catch(err : Error)
			{
				trace("decode error  : " + err + "\n" + err.getStackTrace());
				trace("HexDump : " + Util.dumpHex(buffer));
				if (protocol != null) {
					trace("drop and clean decode state !\n");
					this.uncomplete_package = null;
				}
			}
			
			return true;
		}
		
		protected function encode(protocol : ProtocolImpl) : ByteArray
		{
			try
			{
				var msgType: int = message_factory.getType(protocol.getMessage());
				if (msgType == 0) {
					throw new Error("Encode Error : message type is 0 : " + protocol.getMessage());
				}
				protocol.setSentTime(new Date());
				
				var buffer : ByteArray        = new ByteArray();
				var output : BinNetDataOutput = new BinNetDataOutput(message_factory, buffer);
				
				var oldposition : int = buffer.position;
				{
					// fixed data region
					{
						buffer.writeInt		(protocol_start);		// 4
						buffer.writeInt		(protocol_fixed_size);	// 4
					}
					
					// message data region
					var begin_pos : int = buffer.position;
					{
						buffer.writeByte	(protocol.getProtocol());		// 1
						buffer.writeInt		(protocol.getPacketNumber());	// 4
						
						switch (protocol.getProtocol()) {
						case ProtocolType.PROTOCOL_CHANNEL_JOIN_S2C:
						case ProtocolType.PROTOCOL_CHANNEL_LEAVE_S2C:
						case ProtocolType.PROTOCOL_CHANNEL_MESSAGE:
							buffer.writeInt	(protocol.getChannelID());		// 4
							break;
						case ProtocolType.PROTOCOL_SYSTEM_NOTIFY:
							buffer.writeInt	(protocol.getSystemMessage());
							break;
						}
						
						if (protocol.getMessage() == null) 
						{
							buffer.writeByte	(0);	// 1
						}
						else
						{
							buffer.writeByte	(ProtocolImpl.TRANSMISSION_TYPE_MUTUAL);	// 1
							buffer.writeInt		(msgType);	// ext 4
							message_factory		.writeExternal(protocol.getMessage(), output);
						}
						
					}
					var end_pos : int = buffer.position;
					
					buffer.position = 4;
					
					buffer.writeInt(protocol_fixed_size + (end_pos - begin_pos));
				}
				sentMessage(protocol);
				return buffer;
			}
			catch(err : Error) 
			{
				trace("encode error  : " + err + "\n" + err.getStackTrace());  		
			}
			return null;
		}
		
		
		
		protected function recivedMessage(decoded : Protocol) : void
		{
			// 判断是否是联盟消息，协议消息等
			switch (decoded.getProtocol()) {
				case ProtocolType.PROTOCOL_CHANNEL_JOIN_S2C:{
					this.listener.joinedChannel(decoded, this);
					break;
				}
				case ProtocolType.PROTOCOL_CHANNEL_LEAVE_S2C:{
					this.listener.leftChannel(decoded, this);
					break;
				}
				case ProtocolType.PROTOCOL_CHANNEL_MESSAGE:{
					this.listener.receivedMessage(this, decoded);
					break;
				}
				case ProtocolType.PROTOCOL_SESSION_MESSAGE:{
					this.listener.receivedMessage(this, decoded);
					break;
				}
				case ProtocolType.PROTOCOL_SYSTEM_NOTIFY:{
					this.listener.receivedMessage(this, decoded);
					break;
				}
				default:{
					if (decoded.getMessage() != null) {
						this.listener.receivedMessage(this, decoded);
					}
				}
			}
		}
		
		protected function sentMessage(decoded : Protocol) : void
		{
			this.listener.sentMessage(this, decoded);
		}
		
	}
	
	
}