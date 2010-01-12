package se.l4.sofa.dbus.io.sasl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.security.sasl.SaslClient;
import javax.security.sasl.SaslException;

import se.l4.sofa.dbus.io.Hex;

/**
 * Implementation of DBUS_COOKIE_SHA1 for {@code javax.security.sasl}.
 * 
 * @author Andreas Holstenson
 * 
 */
public class DBusCookieSha1
	implements SaslClient
{
	private enum State
	{
		INITIAL, 
		FIND_COOKIE, 
		DONE
	}

	public static final String NAME = "DBUS_COOKIE_SHA1";

	private String uid;

	private State state;

	public DBusCookieSha1()
	{
		try
		{
			// Try getting the users UID
			Class<?> c = Class
					.forName("com.sun.security.auth.module.UnixSystem");
			Method m = c.getMethod("getUid");
			Object o = c.newInstance();
			long uid = (Long) m.invoke(o);

			this.uid = String.valueOf(uid);
		}
		catch(Exception e)
		{
			this.uid = System.getProperty("user.name");
		}

		state = State.INITIAL;
	}

	public void dispose()
		throws SaslException
	{

	}

	public byte[] evaluateChallenge(byte[] challenge)
		throws SaslException
	{
		switch(state)
		{
			case INITIAL:
				state = State.FIND_COOKIE;
				return uid.getBytes();

			case DONE:
				throw new SaslException("Authentication is done");

			case FIND_COOKIE:
				String[] data = new String(challenge).split(" ");
				if(data.length != 3)
				{
					throw new SaslException("Invalid challenge");
				}

				// Get data from the challenge string
				String context = data[0];
				String cookieId = data[1];
				String serverHash = data[2];

				// Try to find the cookie
				String cookie = getCookie(context, cookieId);

				// Generate client hash
				SecureRandom random = new SecureRandom();
				byte[] randomValue = new byte[256];
				random.nextBytes(randomValue);

				String clientHash = digestAndHex(randomValue);

				// Combine hashes and return result
				String temp = serverHash + ":" + clientHash + ":" + cookie;
				String hash = digestAndHex(temp.getBytes());

				String response = clientHash + " " + hash;

				state = State.DONE;

				return response.getBytes();
		}

		return null;
	}

	private String digestAndHex(byte[] data)
		throws SaslException
	{
		try
		{
			MessageDigest digest = MessageDigest.getInstance("SHA");
			data = digest.digest(data);

			return new String(Hex.encodeHex(data));
		}
		catch(NoSuchAlgorithmException e)
		{
			throw new SaslException("SHA-1 algorithm unavailable", e);
		}
	}

	private String getCookie(String context, String cookieId)
		throws SaslException
	{
		long time = System.currentTimeMillis() / 1000;

		String home = System.getProperty("user.home");
		File f = new File(home + "/.dbus-keyrings/" + context);
		if(false == f.exists())
		{
			throw new SaslException("Given cookie context " + context + " in "
					+ context + " does not exist");
		}

		BufferedReader reader = null;
		try
		{
			reader = new BufferedReader(new FileReader(f));

			String line;
			while((line = reader.readLine()) != null)
			{
				String[] split = line.split(" ");

				// Line must be split in 3 and the cookie id must match
				if(split.length != 3 || false == split[0].equals(cookieId))
				{
					continue;
				}

				// Return the cookie hash
				return split[2];
			}
		}
		catch(IOException e)
		{
			if(e instanceof SaslException)
			{
				throw (SaslException) e;
			}
			else
			{
				throw new SaslException("Unable to read cookie " + cookieId
					+ " in " + context, e);
			}
		}
		finally
		{
			if(reader != null)
			{
				try
				{
					reader.close();
				}
				catch(IOException e)
				{
				}
			}
		}

		throw new SaslException("Unable to find cookie " + cookieId + " in "
				+ context);
	}

	public String getMechanismName()
	{
		return NAME;
	}

	public Object getNegotiatedProperty(String propName)
	{
		return null;
	}

	public boolean hasInitialResponse()
	{
		return true;
	}

	public boolean isComplete()
	{
		return state == State.DONE;
	}

	public byte[] unwrap(byte[] incoming, int offset, int len)
		throws SaslException
	{
		byte[] result = new byte[len];
		System.arraycopy(incoming, offset, result, 0, len);
		
		return result;
	}

	public byte[] wrap(byte[] outgoing, int offset, int len)
		throws SaslException
	{
		byte[] result = new byte[len];
		System.arraycopy(outgoing, offset, result, 0, len);
		
		return result;
	}

}
