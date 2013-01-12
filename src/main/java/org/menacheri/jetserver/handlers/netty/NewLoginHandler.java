package org.menacheri.jetserver.handlers.netty;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandler.Sharable;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.util.CharsetUtil;
import org.menacheri.jetserver.app.GameRoom;
import org.menacheri.jetserver.app.Player;
import org.menacheri.jetserver.app.PlayerSession;
import org.menacheri.jetserver.app.impl.Sessions;
import org.menacheri.jetserver.communication.NettyTCPMessageSender;
import org.menacheri.jetserver.event.Event;
import org.menacheri.jetserver.event.Events;
import org.menacheri.jetserver.server.netty.AbstractNettyServer;
import org.menacheri.jetserver.service.LookupService;
import org.menacheri.jetserver.service.SessionRegistryService;
import org.menacheri.jetserver.util.Credentials;
import org.menacheri.jetserver.util.NettyUtils;
import org.menacheri.jetserver.util.SimpleCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//import com.google.gson.Gson;
//import com.google.gson.JsonArray;
//import com.google.gson.JsonParser;


@Sharable
public class NewLoginHandler extends SimpleChannelUpstreamHandler
{
	private static final Logger LOG = LoggerFactory
			.getLogger(NewLoginHandler.class);

	protected LookupService lookupService;
	protected SessionRegistryService sessionRegistryService;
	/**
	 * Used for book keeping purpose. It will count all open channels. Currently
	 * closed channels will not lead to a decrement.
	 */
	private static final AtomicInteger CHANNEL_COUNTER =  new AtomicInteger(0);
	
	@Override
	public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e)
			throws Exception {
		AbstractNettyServer.ALL_CHANNELS.add(e.getChannel());
		LOG.debug("Added Channel with id: {} as the {}th open channel", e
				.getChannel().getId(), CHANNEL_COUNTER.incrementAndGet());
	}
	
	/**
	 * We sent and receive json strings currently, and later we would prepend 
	 * some codes before this handler for some rubost net protocol, msgpack or
	 * easy compress would be among considerations.
	 * */
	public void messageReceived(final ChannelHandlerContext ctx, final MessageEvent e)
			throws Exception
	{
		ChannelBuffer buffer = (ChannelBuffer) e.getMessage();
		Channel channel = e.getChannel();
		//Client flow administration
//		      transferredBytes.addAndGet(buffer.readableBytes());
        System.out.println("Server get message :"+buffer.toString(CharsetUtil.UTF_8)+"from CHannel %"+ctx.getChannel().getId());        
        String json = buffer.toString(CharsetUtil.UTF_8);
        //	Parse the json string
//        Gson gson = new Gson();
//        JsonParser parser = new JsonParser();
//        JsonArray array = parser.parse(json).getAsJsonArray();
//        String command = gson.fromJson(array.get(0), String.class);
        //Then the player gson serializer would be customized.
        
        //  MyClass event = gson.fromJson(array.get(2), MyClass.class);
//        LOG.debug("LOgin handler get message: {}", command);
//    	String playerInfoString = gson.fromJson(array.get(1), String.class);
//    	array = parser.parse(json).getAsJsonArray();
//    	String userName = gson.fromJson(array.get(0), String.class);
//    	String password = gson.fromJson(array.get(1), String.class);
//    	String loginType = gson.fromJson(array.get(2), String.class);
    	
    	/**The protocol parsing above was really tedious...
    	 * try thrift or protobuf or messagePack later?
    	 * */
        String failureInfo = null;
//        Player player = Player.createPlayer(userName,password,loginType);
    	
//        if (command.equals("signup")) {
        	LOG.trace("SignUp attempt from {}",channel.getRemoteAddress());
//        	failureInfo =!player.trySignUp();        	
//		}
//        else if(command.equals("login")){			
			LOG.trace("Login attempt from {}",channel.getRemoteAddress());
			//Here the game transfered to new stage: the session
			//First of all, we need a DAO service to lookup the user
			/**So we name the PlayerInfo as a service name which can 
			render APIs like lookupPlayer|buildPlayer and so on;*/
			// Here we just use the json to initiate a player object.
			// and it should validate it self through the DAO interface
			// automatically, as we get a invalide we would return failure
			// message to the client.			
//        	failureInfo =player.validate();
//		}
		
		if(failureInfo.equals("")){			
	        // here the player just have two fields: name| password
			// and it's valid, we then load the database datas into the object, 
	        // so called 'Instantialization', and put it into the session.
//			player.instantialize();
			// and the player's full info should be transfered fully to the client.
			// with this message
			channel.write(NettyUtils
					.createBufferForOpcode(Events.LOG_IN_SUCCESS));

//			handleGameRoomJoin(player, channel,buffer);			
			// as the pipeline changed, we would never be in this handler again.
			return;
		}
	
		//If we fall through , we are with failure.		
		LOG.error("Invalid event {} sent from remote address {}. "
					+ "Going to close channel {}",
					new Object[] {  channel.getRemoteAddress(),channel.getId() }
					);
		closeChannelWithLoginFailure(channel,failureInfo);
		
	}


	
	public Player lookupPlayer(final ChannelBuffer buffer, final Channel channel)
	{
		Credentials credentials = new SimpleCredentials(buffer);
		Player player = lookupService.playerLookup(credentials);
		if(null == player){
			LOG.error("Invalid credentials provided by user: {}",credentials);
		}
		return player;
	}
	
	/**
	 * Helper method which will close the channel after writing
	 * {@link Events#LOG_IN_FAILURE} to remote connection.
	 * 
	 * @param channel
	 *            The tcp connection to remote machine that will be closed.
	 */
	private void closeChannelWithLoginFailure(Channel channel, String message)
	{
		// should carry the failure Message.
		ChannelFuture future = channel.write(NettyUtils
				.createBufferForOpcode(Events.LOG_IN_FAILURE));
		future.addListener(ChannelFutureListener.CLOSE);
	}
	
	public void handleGameRoomJoin(Player player, Channel channel, ChannelBuffer buffer)
	{		
		String refKey = NettyUtils.readString(buffer);	
		GameRoom gameRoom = lookupService.gameRoomLookup(refKey);
		PlayerSession playerSession = gameRoom.createPlayerSession(player);
		gameRoom.onLogin(playerSession);
		LOG.trace("Sending GAME_ROOM_JOIN_SUCCESS to channel {}", channel.getId());
		ChannelFuture future = channel.write(NettyUtils.createBufferForOpcode(Events.GAME_ROOM_JOIN_SUCCESS));
		connectToGameRoom(gameRoom,playerSession, future);
		//UDP not now ..
		//		loginUdp(playerSession, buffer);
	}
	
	public void connectToGameRoom(final GameRoom gameRoom, final PlayerSession playerSession, ChannelFuture future)
	{
		future.addListener(new ChannelFutureListener()
		{
			@Override
			public void operationComplete(ChannelFuture future)
					throws Exception
			{
				Channel channel = future.getChannel();
				LOG.trace("Sending GAME_ROOM_JOIN_SUCCESS to channel {} completed", channel.getId());
				if (future.isSuccess())
				{
					LOG.trace("Going to clear pipeline");
					// Clear the existing pipeline
					NettyUtils.clearPipeline(channel.getPipeline());
					// Set the tcp channel on the session. 
					NettyTCPMessageSender sender = new NettyTCPMessageSender(channel);
					playerSession.setTcpSender(sender);
					// Connect the pipeline to the game room.
					gameRoom.connectSession(playerSession);
					// Send the connect event so that it will in turn send the START event.
					playerSession.onEvent(Events.connectEvent(sender));
				}
				else
				{
					LOG.error("GAME_ROOM_JOIN_SUCCESS message sending to client was failure, channel will be closed");
					channel.close();
				}
			}
		});
	}
	
	
	/**
	 * This method adds the player session to the
	 * {@link SessionRegistryService}. The key being the remote udp address of
	 * the client and the session being the value.
	 * 
	 * @param playerSession
	 * @param buffer
	 *            Used to read the remote address of the client which is
	 *            attempting to connect via udp.
	 */
	protected void loginUdp(PlayerSession playerSession, ChannelBuffer buffer)
	{
		InetSocketAddress remoteAdress = NettyUtils.readSocketAddress(buffer);
		if(null != remoteAdress)
		{
			sessionRegistryService.putSession(remoteAdress, playerSession);
		}
	}
	
	public LookupService getLookupService()
	{
		return lookupService;
	}

	public void setLookupService(LookupService lookupService)
	{
		this.lookupService = lookupService;
	}

	public SessionRegistryService getSessionRegistryService()
	{
		return sessionRegistryService;
	}

	public void setSessionRegistryService(
			SessionRegistryService sessionRegistryService)
	{
		this.sessionRegistryService = sessionRegistryService;
	}

}
