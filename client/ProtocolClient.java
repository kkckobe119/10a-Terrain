package client;

import a3.MyGame;
import java.awt.Color;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Iterator;
import java.util.UUID;
import java.util.Vector;
import org.joml.*;

import tage.*;
import tage.networking.client.GameConnectionClient;

public class ProtocolClient extends GameConnectionClient
{
	private MyGame game;
	private GhostManager ghostManager;
	private UUID id;
	private GhostNPC ghostNPC;
	
	public ProtocolClient(InetAddress remoteAddr, int remotePort, ProtocolType protocolType, MyGame g) throws IOException 
	{	super(remoteAddr, remotePort, protocolType);
		game = g;
		this.id = UUID.randomUUID();
		ghostManager = game.getGhostManager();
	}
	
	public UUID getID() { return id; }

	// ------------- GHOST NPC SECTION --------------

	private void createGhostNPC(Vector3f position) throws IOException
	{	if (ghostNPC == null)
			ghostNPC = new GhostNPC(0, game.getNPCshape(), game.getNPCtexture(), position);
	}

	private void updateGhostNPC(Vector3f position, double gsize)
	{	boolean gs;
		if (ghostNPC == null)
		{	try
			{	createGhostNPC(position);
			}	catch (IOException e)
			{	System.out.println("error creating ghost npc at update");
			}
		}
		ghostNPC.setPosition(position);
		if (gsize == 1.0) gs=false; else gs=true;
		ghostNPC.setSize(gs);
	}
	
	@Override
	protected void processPacket(Object message)
	{	String strMessage = (String)message;
		System.out.println("message received -->" + strMessage);
		String[] messageTokens = strMessage.split(",");
		
		// Game specific protocol to handle the message
		if(messageTokens.length > 0)
		{
			// Handle JOIN message
			// Format: (join,success) or (join,failure)
			if(messageTokens[0].compareTo("join") == 0)
			{	if(messageTokens[1].compareTo("success") == 0)
				{	System.out.println("join success confirmed");
					game.setIsConnected(true);
					sendCreateMessage(game.getPlayerPosition());
				}
				if(messageTokens[1].compareTo("failure") == 0)
				{	System.out.println("join failure confirmed");
					game.setIsConnected(false);
			}	}
			
			// Handle BYE message
			// Format: (bye,remoteId)
			if(messageTokens[0].compareTo("bye") == 0)
			{	// remove ghost avatar with id = remoteId
				// Parse out the id into a UUID
				UUID ghostID = UUID.fromString(messageTokens[1]);
				ghostManager.removeGhostAvatar(ghostID);
			}
			
			// Handle CREATE message
			// Format: (create,remoteId,x,y,z)
			// AND
			// Handle DETAILS_FOR message
			// Format: (dsfr,remoteId,x,y,z)
			if (messageTokens[0].compareTo("create") == 0 || (messageTokens[0].compareTo("dsfr") == 0))
			{	// create a new ghost avatar
				// Parse out the id into a UUID
				UUID ghostID = UUID.fromString(messageTokens[1]);
				
				// Parse out the position into a Vector3f
				Vector3f ghostPosition = new Vector3f(
					Float.parseFloat(messageTokens[2]),
					Float.parseFloat(messageTokens[3]),
					Float.parseFloat(messageTokens[4]));

				try
				{	ghostManager.createGhostAvatar(ghostID, ghostPosition);
				}	catch (IOException e)
				{	System.out.println("error creating ghost avatar");
				}
			}
			
			// Handle WANTS_DETAILS message
			// Format: (wsds,remoteId)
			if (messageTokens[0].compareTo("wsds") == 0)
			{
				// Send the local client's avatar's information
				// Parse out the id into a UUID
				UUID ghostID = UUID.fromString(messageTokens[1]);
				sendDetailsForMessage(ghostID, game.getPlayerPosition());
			}
			
			// Handle MOVE message
			// Format: (move,remoteId,x,y,z)
			if (messageTokens[0].compareTo("move") == 0)
			{
				// move a ghost avatar
				// Parse out the id into a UUID
				UUID ghostID = UUID.fromString(messageTokens[1]);
				
				// Parse out the position into a Vector3f
				Vector3f ghostPosition = new Vector3f(
					Float.parseFloat(messageTokens[2]),
					Float.parseFloat(messageTokens[3]),
					Float.parseFloat(messageTokens[4]));
				
				ghostManager.updateGhostAvatar(ghostID, ghostPosition);
			}

			// // Handle ROTATE message
			// // Format: (move,remoteId,x,y,z)
			if (messageTokens[0].compareTo("rotate") == 0)
			{
				// rotate a ghost avatar
				// Parse out the id into a UUID
				UUID ghostID = UUID.fromString(messageTokens[1]);
				
				// Parse out the position into a Vector3f
				Vector3f ghostRotation = new Vector3f(
					Float.parseFloat(messageTokens[2]),
					Float.parseFloat(messageTokens[3]),
					Float.parseFloat(messageTokens[4]));
				
				ghostManager.updateGhostAvatar(ghostID, ghostRotation);
			}

			// ------------- HANDLE NPC MESSAGES ---------------

			// Handle CREATE_NPC message
			// Format: (createNPC,id,x,y,z,state)
			if (messageTokens[0].compareTo("createNPC") == 0)
			{	// create a new ghost NPC
				// Parse out the position
				Vector3f ghostPosition = new Vector3f(
					Float.parseFloat(messageTokens[1]),
					Float.parseFloat(messageTokens[2]),
					Float.parseFloat(messageTokens[3]));
				try
				{	createGhostNPC(ghostPosition); System.out.println("client creating a ghost NPC");
				}	catch (IOException e)
				{	System.out.println("error creating ghost avatar");
				}
			}

			// Handle MOVE NPC message
			// Format: (mnpc,npcID,x,y,z)
			if(messageTokens[0].compareTo("mnpc") == 0)
			{	// move a ghost npc
				// Parse out the position into a Vector3f
				Vector3f ghostPosition = new Vector3f(
					Float.parseFloat(messageTokens[1]),
					Float.parseFloat(messageTokens[2]),
					Float.parseFloat(messageTokens[3]));
				double gSize = Double.parseDouble(messageTokens[4]);
				updateGhostNPC(ghostPosition, gSize);
			}

			// Handle ROTATE NPC message
			// Format: (rnpc,npcID,x,y,z)
			if(messageTokens[0].compareTo("rnpc") == 0)
			{	// move a ghost npc
				// Parse out the position into a Vector3f
				Vector3f ghostRotation = new Vector3f(
					Float.parseFloat(messageTokens[1]),
					Float.parseFloat(messageTokens[2]),
					Float.parseFloat(messageTokens[3]));
				double gSize = Double.parseDouble(messageTokens[4]);
				updateGhostNPC(ghostRotation, gSize);
			}

			// Handle isNear NPC message
			// Format: (isnr,x,y,z,criteria)
			if(messageTokens[0].compareTo("isnr")==0)
			{	// Parse out the position into a Vector3D
				Vector3f ghostPosition = new Vector3f(
					Float.parseFloat(messageTokens[1]),
					Float.parseFloat(messageTokens[2]),
					Float.parseFloat(messageTokens[3]));
				double criteria = Double.parseDouble(messageTokens[4]);
				Vector3f plLoc = game.getPlayerPosition();
				float dist = ghostPosition.distance(plLoc);
				if (dist < criteria) answerIsNear();
			}
			// -------------------------------------------------------------
		}	
	}
	
	// The initial message from the game client requesting to join the 
	// server. localId is a unique identifier for the client. Recommend 
	// a random UUID.
	// Message Format: (join,localId)
	
	public void sendJoinMessage()
	{	try 
		{	sendPacket(new String("join," + id.toString()));
		} catch (IOException e) 
		{	e.printStackTrace();
	}	}
	
	// Informs the server that the client is leaving the server. 
	// Message Format: (bye,localId)

	public void sendByeMessage()
	{	try 
		{	sendPacket(new String("bye," + id.toString()));
		} catch (IOException e) 
		{	e.printStackTrace();
	}	}
	
	// Informs the server of the client's Avatar's position. The server 
	// takes this message and forwards it to all other clients registered 
	// with the server.
	// Message Format: (create,localId,x,y,z) where x, y, and z represent the position

	public void sendCreateMessage(Vector3f position)
	{	try 
		{	String message = new String("create," + id.toString());
			message += "," + position.x();
			message += "," + position.y();
			message += "," + position.z();
			
			sendPacket(message);
		} catch (IOException e) 
		{	e.printStackTrace();
	}	}
	
	// Informs the server of the local avatar's position. The server then 
	// forwards this message to the client with the ID value matching remoteId. 
	// This message is generated in response to receiving a WANTS_DETAILS message 
	// from the server.
	// Message Format: (dsfr,remoteId,localId,x,y,z) where x, y, and z represent the position.

	public void sendDetailsForMessage(UUID remoteId, Vector3f position)
	{	try 
		{	String message = new String("dsfr," + remoteId.toString() + "," + id.toString());
			message += "," + position.x();
			message += "," + position.y();
			message += "," + position.z();
			
			sendPacket(message);
		} catch (IOException e) 
		{	e.printStackTrace();
	}	}
	
	// Informs the server that the local avatar has changed position.  
	// Message Format: (move,localId,x,y,z) where x, y, and z represent the position.

	public void sendMoveMessage(Vector3f position){
		try {
			String message = new String("move," + id.toString());
			message += "," + position.x();
			message += "," + position.y();
			message += "," + position.z();
			
			sendPacket(message);
		}catch (IOException e) {
			e.printStackTrace();
		}
	}

	// --------------- NPC SECTION --------------------

	public void answerIsNear()
	{	try
		{	sendPacket(new String("isnear," + id.toString()));
		}
		catch (IOException e)
		{	e.printStackTrace();
		}
	}

	public void askForNPCinfo()
	{	try
		{	sendPacket(new String("needNPC," + id.toString()));
		}
		catch (IOException e)
		{	e.printStackTrace();
		}
	}

	// Informs the server that the local avatar has changed direction.  
	// Message Format: (rotate,localId,x,y,z) where x, y, and z represent the position.

	// public void sendMoveMessage(Vector3f position){
	// 	try {
	// 		String message = new String("rotate," + id.toString());
	// 		message += "," + position.x();
	// 		message += "," + position.y();
	// 		message += "," + position.z();
			
	// 		sendPacket(message);
	// 	}catch (IOException e) {
	// 		e.printStackTrace();
	// 	}
	// }
}
