package org.menacheri.jetserver.server.netty;

import static org.jboss.netty.channel.Channels.pipeline;

import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.handler.codec.frame.LengthFieldBasedFrameDecoder;
import org.jboss.netty.handler.codec.frame.LengthFieldPrepender;
import org.jboss.netty.handler.timeout.IdleStateAwareChannelHandler;
import org.jboss.netty.handler.timeout.IdleStateHandler;
import org.jboss.netty.util.Timer;
import org.menacheri.jetserver.handlers.netty.EventDecoder;
import org.menacheri.jetserver.handlers.netty.LoginHandler;
import org.menacheri.jetserver.handlers.netty.NewLoginHandler;

public class NewLoginPipelineFactory implements ChannelPipelineFactory {

	/**
	 * TODO make this configurable
	 */
	private static final int MAX_IDLE_SECONDS = 60;
	private Timer timer;
	private IdleStateAwareChannelHandler idleCheckHandler;
	private NewLoginHandler nloginHandler;

	
	@Override
	public ChannelPipeline getPipeline() throws Exception
	{
		// Create a default pipeline implementation.
		ChannelPipeline pipeline = pipeline();
		addHandlers(pipeline);
		return pipeline;
	}

	public ChannelPipeline addHandlers(ChannelPipeline pipeline)
	{
		if (null == pipeline)
			return null;
		pipeline.addLast("loginHandler", nloginHandler);
		return pipeline;
	}

	public IdleStateAwareChannelHandler getIdleCheckHandler()
	{
		return idleCheckHandler;
	}

	public void setIdleCheckHandler(IdleStateAwareChannelHandler idleCheckHandler)
	{
		this.idleCheckHandler = idleCheckHandler;
	}

	public Timer getTimer()
	{
		return timer;
	}

	public void setTimer(Timer timer)
	{
		this.timer = timer;
	}

}

