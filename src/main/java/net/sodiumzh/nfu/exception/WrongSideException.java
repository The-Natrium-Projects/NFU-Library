package net.sodiumzh.nfu.exception;

import java.io.Serial;

/**
 * Thrown when something should be invoked only on a certain side, but actually invoked on another side.
 */
public class WrongSideException extends RuntimeException
{

	@Serial
	private static final long serialVersionUID = 1990333858334025102L;

	public WrongSideException(String msg)
	{
		super(msg);
	}
	
	public WrongSideException(String msg, Throwable cause)
	{
		super(msg);
	}

}
