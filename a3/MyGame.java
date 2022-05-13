package a3;

import client.*;
import actions.*;
import tage.*;
import tage.audio.AudioManagerFactory;
import tage.audio.AudioResource;
import tage.audio.AudioResourceType;
import tage.audio.IAudioManager;
import tage.audio.Sound;
import tage.audio.SoundType;
import tage.shapes.*;
import tage.input.*;
import tage.input.action.*;

import java.lang.Math;
import java.util.Random;
import java.awt.*;

import java.awt.event.*;

import java.io.*;
import javax.swing.*;
import org.joml.*;

import net.java.games.input.*;
import net.java.games.input.Component.Identifier.*;

import java.net.InetAddress;

import java.net.UnknownHostException;
import net.java.games.input.*;
import net.java.games.input.Component.Identifier.*;
import tage.networking.IGameConnection.ProtocolType;

import tage.physics.PhysicsEngine;
import tage.physics.PhysicsObject;
import tage.physics.PhysicsEngineFactory;
import tage.physics.JBullet.*;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.collision.dispatch.CollisionObject;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import java.util.*;
import java.util.List;

public class MyGame extends VariableFrameRateGame
{
	private static Engine engine;
	private InputManager im;
	private GhostManager gm;
	private IAudioManager audioMgr; 
 	private Sound beeSound;

	private GameObject ball1, ball2, plane;
	private PhysicsEngine physicsEngine;
	private PhysicsObject ball1P, ball2P, planeP, avatarP, honeyPotP;

	private GameObject[] balls = new GameObject[125];
	private PhysicsObject[] ballsP = new PhysicsObject[125];

	private int counter=0;
	private Vector3f currentPosition;
	private double startTime, prevTime, elapsedTime, amt;
	private boolean running = false;

	private GameObject ZAxis, XAxis, YAxis, terr, avatar, honeyPot, bees, sHoneyPot;
	private ObjShape ghostS, avatarS, terrS, line1, line2, line3, honeyPotS, sphS, beesS, npcShape;
	private AnimatedShape bearS;
	private TextureImage ghostTx, avatarTx, hills, grass, honeyPotT, beesTx, npcTx;
	private Light lightP, lightP2;
	private int fluffyClouds, lakeIslands; // skyboxes
	private CameraOrbit3D orbitController;

	private String serverAddress, gpName;
	private int serverPort;
	private ProtocolType serverProtocol;
	private ProtocolClient protClient;
	private boolean isClientConnected = false, visible = false;
	private double test;

	private float avatarX;
	private float avatarY;
	private float avatarZ;
	private float avatarRot;

	private float terrainPos;
	private float terrainScaleX;
	private float terrainScaleY;
	private float terrainScaleZ;

	private float honeyPotX;
	private float honeyPotY;
	private float honeyPotZ;
	private float honeyPotRot;

	private float sHoneyPotX;
	private float sHoneyPotY;
	private float sHoneyPotZ;
	private float sHoneyPotRot;

	private float beesX;
	private float beesY;
	private float beesZ;
	private float beesRot;
	Random r = new Random();

	private boolean gameOverBool = false;

	private float vals[] = new float[16];

	public MyGame(String serverAddress, int serverPort, String protocol) { 
		super();
		gm = new GhostManager(this);
		this.serverAddress = serverAddress;
		this.serverPort = serverPort;
		if (protocol.toUpperCase().compareTo("TCP") == 0)
			this.serverProtocol = ProtocolType.TCP;
		else
			this.serverProtocol = ProtocolType.UDP;
	}

	public static void main(String[] args){
		MyGame game = new MyGame(args[0], Integer.parseInt(args[1]), args[2]);

		ScriptEngineManager factory = new ScriptEngineManager();
		String scriptFileName = "scripts/initialize.js";

		// get a list of the script engines on this platform
		List<ScriptEngineFactory> list = factory.getEngineFactories();
	
		System.out.println("Script Engine Factories found:");
		for (ScriptEngineFactory f : list)
		{ System.out.println("  Name = " + f.getEngineName()
						   + "  language = " + f.getLanguageName()
						   + "  extensions = " + f.getExtensions());
		}
	
		// get the JavaScript engine
		ScriptEngine jsEngine = factory.getEngineByName("js");
		// run the script
		game.executeScript(jsEngine, scriptFileName);
		engine = new Engine(game);
		game.initializeSystem();
		game.game_loop();
	}

	private void executeScript(ScriptEngine engine, String scriptFileName){
	  try{
		FileReader fileReader = new FileReader(scriptFileName);
		engine.eval(fileReader);         //execute all the script statements in the file
		avatarX = ((Double)(engine.get("avatarX"))).floatValue();
		avatarY = ((Double)(engine.get("avatarY"))).floatValue();
		avatarZ = ((Double)(engine.get("avatarZ"))).floatValue();
		avatarRot = ((Double)(engine.get("avatarRot"))).floatValue();

		honeyPotX = ((Double)(engine.get("honeyPotX"))).floatValue();
		honeyPotY = ((Double)(engine.get("honeyPotY"))).floatValue();
		honeyPotZ = ((Double)(engine.get("honeyPotZ"))).floatValue();
		honeyPotRot = ((Double)(engine.get("honeyPotRot"))).floatValue();

		sHoneyPotX = honeyPotX;
		sHoneyPotY = honeyPotY;
		sHoneyPotZ = honeyPotZ;
		sHoneyPotRot = honeyPotRot;

		terrainPos = ((Double)(engine.get("terrainPos"))).floatValue();
		terrainScaleX = ((Double)(engine.get("terrainScaleX"))).floatValue();
		terrainScaleY = ((Double)(engine.get("terrainScaleY"))).floatValue();
		terrainScaleZ = ((Double)(engine.get("terrainScaleZ"))).floatValue();

		fileReader.close();
	  }
	  catch (FileNotFoundException e1)
	  { System.out.println(scriptFileName + " not found " + e1); }
	  catch (IOException e2)
	  { System.out.println("IO problem with " + scriptFileName + e2); }
	  catch (ScriptException e3) 
	  { System.out.println("ScriptException in " + scriptFileName + e3); }
	  catch (NullPointerException e4)
	  { System.out.println ("Null ptr exception reading " + scriptFileName + e4); }
	}

	@Override
	public void loadShapes(){
		avatarS = new ImportedModel("bear.obj");
		
		terrS = new TerrainPlane(1000);
		ghostS = new ImportedModel("bear.obj"); /*new ImportedModel("dolphinHighPoly.obj");*/
		//bearS = new AnimatedShape("bear.rkm", "bear.rks"); 
  		//bearS.loadAnimation("WALK", "walk.rka"); 

		honeyPotS = new ImportedModel("honeyPot.obj");
		sphS = new Sphere();
		beesS = new ImportedModel("dolphinHighPoly.obj");
		npcShape = new ImportedModel("bear.obj");
		line1 = new Line(new Vector3f(-999999.0f, 0.0f, 0.0f) , new Vector3f(999999.0f, 0.0f, 0.0f));
        line2 = new Line(new Vector3f(0.0f, -999999.0f, 0.0f) , new Vector3f(0.0f, 999999.0f, 0.0f));
        line3 = new Line(new Vector3f(0.0f, 0.0f, -999999.0f) , new Vector3f(0.0f, 0.0f, 999999.0f));
	}

	@Override
	public void loadTextures()
	{	avatarTx = new TextureImage("bearUV.png");
		ghostTx = new TextureImage("bearUV.png");
		hills = new TextureImage("hills3.jpg");
		grass = new TextureImage("grass.jpg");
		honeyPotT = new TextureImage("pot_color.png");
		beesTx = honeyPotT;
		npcTx = new TextureImage("grass.jpg");
	}

	@Override
	public void buildObjects()
	{	Matrix4f initialTranslation, initialRotation, initialScale;

		// build dolphin avatar
		avatar = new GameObject(GameObject.root(), avatarS, avatarTx); //avatar = new GameObject(GameObject.root(), avatarS, avatarTx);
		initialTranslation = (new Matrix4f()).translation(avatarX, avatarY, avatarZ);
		avatar.setLocalTranslation(initialTranslation);
		initialRotation = (new Matrix4f()).rotationY((float)java.lang.Math.toRadians(avatarRot));
		initialScale = (new Matrix4f()).scaling(0.5f, 0.5f, 0.5f);
		avatar.setLocalScale(initialScale);
		avatar.setLocalRotation(initialRotation);

		// build honeyPot object
		honeyPot = new GameObject(GameObject.root(), honeyPotS, honeyPotT);
		initialTranslation = (new Matrix4f()).translation(honeyPotX, honeyPotY, honeyPotZ);
		honeyPot.setLocalTranslation(initialTranslation);
		initialRotation = (new Matrix4f()).rotationY((float)java.lang.Math.toRadians(honeyPotRot));
		honeyPot.setLocalRotation(initialRotation);

		// build sHoneyPot object
		sHoneyPot = new GameObject(GameObject.root(), honeyPotS, honeyPotT);
		initialTranslation = (new Matrix4f()).translation(sHoneyPotX-40.0f, sHoneyPotY-3, sHoneyPotZ-0.5f);
		sHoneyPot.setLocalTranslation(initialTranslation);
		sHoneyPot.setParent(honeyPot);
		sHoneyPot.propagateTranslation(true);
		sHoneyPot.propagateRotation(true);
		initialScale = (new Matrix4f()).scaling(0.25f, 0.25f, 0.25f);
		sHoneyPot.setLocalScale(initialScale);
		initialRotation = (new Matrix4f()).rotationY((float)java.lang.Math.toRadians(sHoneyPotRot));
		sHoneyPot.setLocalRotation(initialRotation);

		// build terrain object
		terr = new GameObject(GameObject.root(), terrS, grass);
		initialTranslation = (new Matrix4f()).translation(terrainPos,terrainPos,terrainPos);
		terr.setLocalTranslation(initialTranslation);
		initialScale = (new Matrix4f()).scaling(terrainScaleX, terrainScaleY, terrainScaleZ);
		terr.setLocalScale(initialScale);
		terr.setHeightMap(hills);

		// Set bees location again
		for(int i=0;i<balls.length;i++){
			balls[i] = new GameObject(GameObject.root(), beesS, beesTx);
			balls[i].setLocalTranslation((new Matrix4f()).translation(r.nextInt(300)+100, 10.0f, r.nextInt(50)-25));
			balls[i].setLocalScale((new Matrix4f()).scaling(0.75f));
		}
	}

	@Override
	public void initializeGame(){
		//System.out.println(avatarX + " " + avatarY + " " + avatarZ);

		prevTime = System.currentTimeMillis();
		startTime = System.currentTimeMillis();
		(engine.getRenderSystem()).setWindowDimensions(1900,1000);

		initAudio(); 

		//----------------- adding light -----------------
		Light.setGlobalAmbient(.5f, .5f, .5f);

		lightP = new Light();
		lightP.setLocation(new Vector3f(-90.0f, 5f, 0f));
		(engine.getSceneGraph()).addLight(lightP);

		lightP2 = new Light();
		lightP2.setLocation(new Vector3f(35.0f, 5f, 0f));
		(engine.getSceneGraph()).addLight(lightP2);

		// ----------------- INPUTS SECTION -----------------------------
	
		im = engine.getInputManager();
		String gpName = im.getFirstGamepadName();
		String kbName = im.getKeyboardName();

		// ----------------- initialize camera ----------------
		Camera c = (engine.getRenderSystem()).getViewport("MAIN").getCamera();
		orbitController = new CameraOrbit3D(c, avatar, kbName, engine);

		//------------- PHYSICS --------------

		//     --- initialize physics system ---
		String engine = "tage.physics.JBullet.JBulletPhysicsEngine";
		float[] gravity = {-5f, 0f, 0f};
		physicsEngine = PhysicsEngineFactory.createPhysicsEngine(engine);
		physicsEngine.initSystem();
		physicsEngine.setGravity(gravity);

		//     --- create physics world ---
		float mass = 1.0f;
		float up[] = {0,1,0};
		double[] tempTransform;

		Matrix4f translation = new Matrix4f(terr.getLocalTranslation());
		tempTransform = toDoubleArray(translation.get(vals));
		planeP = physicsEngine.addStaticPlaneObject(physicsEngine.nextUID(), tempTransform, up, 0.0f);
		planeP.setBounciness(1.0f);
		terr.setPhysicsObject(planeP);

		translation = new Matrix4f(avatar.getLocalTranslation());
		tempTransform = toDoubleArray(translation.get(vals));
		avatarP = physicsEngine.addSphereObject(physicsEngine.nextUID(), mass, tempTransform, 0.75f);
		avatarP.setBounciness(0.0f);

		translation = new Matrix4f(honeyPot.getLocalTranslation());
		tempTransform = toDoubleArray(translation.get(vals));
		honeyPotP = physicsEngine.addSphereObject(physicsEngine.nextUID(), mass, tempTransform, 0.75f);
		honeyPotP.setBounciness(0.0f);

		for(int i=0; i<balls.length; i++){
			translation = new Matrix4f(balls[i].getLocalTranslation());
			tempTransform = toDoubleArray(translation.get(vals));
			ballsP[i] = physicsEngine.addSphereObject(physicsEngine.nextUID(), mass, tempTransform, 0.75f);
			ballsP[i].setBounciness(1.0f);
			balls[i].setPhysicsObject(ballsP[i]);
		}

		// Matrix4f translation = new Matrix4f(ball1.getLocalTranslation());
		// tempTransform = toDoubleArray(translation.get(vals));
		// ball1P = physicsEngine.addSphereObject(physicsEngine.nextUID(), mass, tempTransform, 0.75f);
		// ball1P.setBounciness(1.0f);
		// ball1.setPhysicsObject(ball1P);
		
		// translation = new Matrix4f(ball2.getLocalTranslation());
		// tempTransform = toDoubleArray(translation.get(vals));
		// ball2P = physicsEngine.addSphereObject(physicsEngine.nextUID(), mass, tempTransform, 0.75f);
		// ball2P.setBounciness(1.0f);
		// ball2.setPhysicsObject(ball2P);

		//avatar.setPhysicsObject(avatarP);

		setupNetworking();

	// build some action objects for doing things in response to user input
		MoveAction moveAction = new MoveAction(this, protClient);
		TurnAction turnAction = new TurnAction(this, protClient);
		PanCameraAction panCameraAction = new PanCameraAction(this);
		ZoomCameraAction zoomCameraAction = new ZoomCameraAction(this);
		RenderLinesAction renderLinesAction = new RenderLinesAction(this);
		TogglePhysicsAction togglePhysicsAction = new TogglePhysicsAction(this);

		ArrayList<Controller> controllers = im.getControllers();

		for (Controller con : controllers){
			if (con.getType() == Controller.Type.KEYBOARD){
				//Dolphin Movement Controls
				im.associateAction(con, net.java.games.input.Component.Identifier.Key.W,
				moveAction, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
				im.associateAction(con, net.java.games.input.Component.Identifier.Key.S,
				moveAction, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
				
				im.associateAction(con, net.java.games.input.Component.Identifier.Key.A,
				turnAction, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
				im.associateAction(con, net.java.games.input.Component.Identifier.Key.D,
				turnAction, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

				// //Toggle Lines
				// im.associateAction(con, net.java.games.input.Component.Identifier.Key.Z,
				// renderLinesAction, InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);

				//Toggle Physics
				im.associateAction(con, net.java.games.input.Component.Identifier.Key.SPACE,
				togglePhysicsAction, InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
				
			}else if(con.getType() == Controller.Type.GAMEPAD || con.getType() == Controller.Type.STICK){
				// Dolphin Movement Controls 
				im.associateAction(con, net.java.games.input.Component.Identifier.Axis.Y,
				moveAction, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
				im.associateAction(con, net.java.games.input.Component.Identifier.Axis.RX, 
				turnAction, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN); 

				// im.associateAction(gpName, net.java.games.input.Component.Identifier.Button._0, 
				// zoomCameraAction, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
				// im.associateAction(gpName, net.java.games.input.Component.Identifier.Button._3, 
				// zoomCameraAction, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

				// im.associateAction(gpName, net.java.games.input.Component.Identifier.Button._1, 
				// panCameraAction, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
				// im.associateAction(gpName, net.java.games.input.Component.Identifier.Button._2, 
				// panCameraAction, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);

			}
		}
	}

	public void initAudio() 
 	{ 
		 AudioResource resource1, resource2; 
  		 audioMgr = AudioManagerFactory.createAudioManager( 
         "tage.audio.joal.JOALAudioManager"); 
  		 if (!audioMgr.initialize()) { 
			   System.out.println("Audio Manager failed to initialize!"); 
   		 return; 
  		 } 
  		resource1 = audioMgr.createAudioResource("assets/sounds/bee.wav", AudioResourceType.AUDIO_SAMPLE); 
  		//resource2 = audioMgr.createAudioResource( "assets/sounds/ocean.wav", AudioResourceType.AUDIO_SAMPLE); 
  		beeSound = new Sound(resource1, SoundType.SOUND_EFFECT, 0, true); 
  		//oceanSound = new Sound(resource2, SoundType.SOUND_EFFECT, 100, true); 
  		beeSound.initialize(audioMgr); 
  		//oceanSound.initialize(audioMgr); 
  		beeSound.setMaxDistance(10.0f); 
  		beeSound.setMinDistance(0.5f); 
  		beeSound.setRollOff(5.0f); 
  		//oceanSound.setMaxDistance(10.0f); 
  		//oceanSound.setMinDistance(0.5f); 
  		//oceanSound.setRollOff(5.0f); 
  		//beeSound.setLocation(ball1.getWorldLocation()); 
		for(int i=0;i<balls.length;i++){
			beeSound.setLocation(balls[i].getWorldLocation()); 
		}
  		//oceanSound.setLocation(rainTorus.getWorldLocation()); 
  		setEarParameters(); 
  		beeSound.play(); 
 	} 

 	public void setEarParameters() 
 	{ 
		Camera camera = (engine.getRenderSystem()).getViewport("MAIN").getCamera(); 
  		audioMgr.getEar().setLocation(avatar.getWorldLocation()); 
  		audioMgr.getEar().setOrientation(camera.getN(), new Vector3f(0.0f, 1.0f, 0.0f)); 
 	} 

	public void toggleAxis(){
        if(visible == false){
            XAxis = new GameObject(GameObject.root(),line1);
            YAxis = new GameObject(GameObject.root(), line2);
            ZAxis = new GameObject(GameObject.root(), line3);
            (XAxis.getRenderStates()).setColor(new Vector3f(1f,0f,0f)); 
            (YAxis.getRenderStates()).setColor(new Vector3f(0f,1f,0f)); 
            (ZAxis.getRenderStates()).setColor(new Vector3f(0f,0f,1f));
            visible = true;
        } else {
            GameObject.root().removeChild(XAxis);
            GameObject.root().removeChild(YAxis);
            GameObject.root().removeChild(ZAxis); 
            visible = false; 
        } 
    }

	public void togglePhysics(){
		if(running == false){
			running = true;
			System.out.println("--- Physics on ---");
		}else{
			running = false;
			System.out.println("--- Physics off ---");
		}
	}

	public void yaw(float speed, float con){
		avatar.setLocalRotation(avatar.getLocalRotation().rotateY((float) Math.toRadians(con*speed)));
   }

	@Override
	public void loadSkyBoxes()
	{	lakeIslands = (engine.getSceneGraph()).loadCubeMap("lakeIslands");
		(engine.getSceneGraph()).setActiveSkyBoxTexture(lakeIslands);
		(engine.getSceneGraph()).setSkyBoxEnabled(true);
	}

	public GameObject getAvatar() { return avatar; }

	@Override
	public void update()
	{	elapsedTime = System.currentTimeMillis() - prevTime;
		prevTime = System.currentTimeMillis();
		amt += elapsedTime;
		Camera c = (engine.getRenderSystem()).getViewport("MAIN").getCamera();
		
		// build and set HUD
		int elapsTimeSec = Math.round((float)(System.currentTimeMillis()-startTime)/1000.0f);
		String elapsTimeStr = Integer.toString(elapsTimeSec);
		String counterStr = Integer.toString(counter);
		String dispStr1 = "Time = " + elapsTimeStr;
		String dispStr2 = "camera position = "
			+ (c.getLocation()).x()
			+ ", " + (c.getLocation()).y()
			+ ", " + (c.getLocation()).z();
		Vector3f hud1Color = new Vector3f(0,0,1);
		Vector3f hud2Color = new Vector3f(1,1,1);
		(engine.getHUDmanager()).setHUD1(dispStr1, hud1Color, 15, 15);
		(engine.getHUDmanager()).setHUD2(dispStr2, hud2Color, 500, 15);

		// update inputs and camera
		im.update((float)elapsedTime);

		//update altitude of dolphin based on height map
		// Vector3f loc = dolphin.getWorldLocation();
		// float height = terr.getHeight(loc.x(), loc.z());
		// dolphin.setLocalLocation(new Vector3f(loc.x(), height, loc.z()));

		orbitController.updateCameraPosition();
		processNetworking((float)elapsedTime);

		//update altitude of dolphin based on height map
		Vector3f loc = avatar.getWorldLocation();
		float height = terr.getHeight(loc.x(), loc.z());
		avatar.setLocalLocation(new Vector3f(loc.x(), height+0.5f, loc.z()));
		avatarP.setTransform(toDoubleArray(new Matrix4f(avatar.getLocalTranslation()).get(vals)));

		if(!gameOverBool){
			for(int i=0;i<balls.length;i++){
				loc = balls[i].getWorldLocation();
				height = terr.getHeight(loc.x(), loc.z());
				balls[i].setLocalLocation(new Vector3f(loc.x(), height+1, loc.z()));
				ballsP[i].setTransform(toDoubleArray(new Matrix4f(balls[i].getLocalTranslation()).get(vals)));
			}
		}
		for(int i=0;i<balls.length;i++){
			loc = balls[i].getWorldLocation();
			height = terr.getHeight(loc.x(), loc.z());
			balls[i].setLocalLocation(new Vector3f(loc.x(), height+1, loc.z()));
			ballsP[i].setTransform(toDoubleArray(new Matrix4f(balls[i].getLocalTranslation()).get(vals)));
		}

		// terrain follow
		loc = honeyPot.getWorldLocation();
		height = terr.getHeight(loc.x(), loc.z());
		honeyPot.setLocalLocation(new Vector3f(loc.x(), height, loc.z()));
		honeyPotP.setTransform(toDoubleArray(new Matrix4f(honeyPot.getLocalTranslation()).get(vals)));


		for(int i=0;i<balls.length;i++){
			loc = balls[i].getWorldLocation();
			if (balls[i].getWorldLocation().x() < -89f){
				//balls[i].setLocalLocation(new Vector3f(200f, loc.y(), loc.z()));
				balls[i].setLocalTranslation((new Matrix4f()).translation(100, loc.y(), r.nextInt(50)-25));
				ballsP[i].setTransform(toDoubleArray(new Matrix4f(balls[i].getLocalTranslation()).get(vals)));
			}
		}

		if(running)
		{	Matrix4f mat = new Matrix4f();
			Matrix4f mat2 = new Matrix4f().identity();
			checkForCollisions();
			physicsEngine.update((float)elapsedTime);
			for (GameObject go:engine.getSceneGraph().getGameObjects())
			{	if (go.getPhysicsObject() != null)
				{	mat.set(toFloatArray(go.getPhysicsObject().getTransform()));
					mat2.set(3,0,mat.m30()); mat2.set(3,1,mat.m31()); mat2.set(3,2,mat.m32());
					go.setLocalTranslation(mat2);
				}
			}
		}

		// update sound 
		// beeSound.setLocation(ball1.getWorldLocation()); 

		for(int i=0;i<balls.length;i++){
			beeSound.setLocation(balls[i].getWorldLocation()); 
		}

		//oceanSound.setLocation(rainTorus.getWorldLocation()); 
		setEarParameters(); 
		//bearS.updateAnimation();

		if(avatar.getWorldLocation().z() > 22){
            avatar.setLocalLocation(new Vector3f(avatar.getWorldLocation().x(), avatar.getWorldLocation().y(), 22f));
        }else if(avatar.getWorldLocation().z() < -22){
            avatar.setLocalLocation(new Vector3f(avatar.getWorldLocation().x(), avatar.getWorldLocation().y(), -22f));
        }

		if(avatar.getWorldLocation().x() > 40){
            avatar.setLocalLocation(new Vector3f(40, avatar.getWorldLocation().y(), avatar.getWorldLocation().z()));
        }else if(avatar.getWorldLocation().x() < -91){
            avatar.setLocalLocation(new Vector3f(-91, avatar.getWorldLocation().y(), avatar.getWorldLocation().z()));
        }

		for(int i = 1; i < balls.length; i++){
            Vector3f test = balls[i].getWorldLocation();
            Vector3f avtest = avatar.getWorldLocation();

            if(Math.abs(avtest.x() - test.x()) <= 0.9 && Math.abs(avtest.z() - test.z()) <= 0.9){
                avatar.setLocalLocation(new Vector3f(-90,avatar.getWorldLocation().y(), avatar.getWorldLocation().z()));
            }
        }


		if(Math.abs(avatar.getWorldLocation().x() - honeyPot.getWorldLocation().x()) <= 0.9 && Math.abs(avatar.getWorldLocation().z() - honeyPot.getWorldLocation().z()) <= 0.9){
			gameOverBool = true;
		}

		if(gameOverBool){
			for(int i = 1; i < balls.length; i++){
				balls[i].setLocalLocation(new Vector3f(balls[i].getWorldLocation().x(), balls[i].getWorldLocation().y() + 1.0f, balls[i].getWorldLocation().z()));
			}
		}
	}

	public void playWalk(){
		//bearS.stopAnimation(); 
    	//bearS.playAnimation("WALK", 0.5f, AnimatedShape.EndType.LOOP, 0); 
	}

	public void stopWalk(){
		//bearS.stopAnimation(); 
	}

	private void checkForCollisions(){
		com.bulletphysics.dynamics.DynamicsWorld dynamicsWorld;
		com.bulletphysics.collision.broadphase.Dispatcher dispatcher;
		com.bulletphysics.collision.narrowphase.PersistentManifold manifold;
		com.bulletphysics.dynamics.RigidBody object1, object2;
		com.bulletphysics.collision.narrowphase.ManifoldPoint contactPoint;

		dynamicsWorld = ((JBulletPhysicsEngine)physicsEngine).getDynamicsWorld();
		dispatcher = dynamicsWorld.getDispatcher();
		int manifoldCount = dispatcher.getNumManifolds();
		for (int i=0; i<manifoldCount; i++){
			manifold = dispatcher.getManifoldByIndexInternal(i);
			object1 = (com.bulletphysics.dynamics.RigidBody)manifold.getBody0();
			object2 = (com.bulletphysics.dynamics.RigidBody)manifold.getBody1();
			JBulletPhysicsObject obj1 = JBulletPhysicsObject.getJBulletPhysicsObject(object1);
			JBulletPhysicsObject obj2 = JBulletPhysicsObject.getJBulletPhysicsObject(object2);
			for (int j = 0; j < manifold.getNumContacts(); j++){
				contactPoint = manifold.getContactPoint(j);
				if (contactPoint.getDistance() < 0.0f){
					System.out.println("---- hit between " + obj1 + " and " + obj2);
					//System.out.println("Avatar UID: " + avatarP.getUID());
					break;
				}
			}
		}
	}

	public double getElapsedTime() {
		return elapsedTime;
	}

	// ------------------ UTILITY FUNCTIONS used by physics

	private float[] toFloatArray(double[] arr)
	{	if (arr == null) return null;
		int n = arr.length;
		float[] ret = new float[n];
		for (int i = 0; i < n; i++){
			ret[i] = (float)arr[i];
		}
		return ret;
	}
	
	private double[] toDoubleArray(float[] arr){
		if (arr == null){
			return null;
		}
		int n = arr.length;
		double[] ret = new double[n];
		for (int i = 0; i < n; i++){
			ret[i] = (double)arr[i];
		}
		return ret;
	}
	// ---------- NPC/AI SECTION ----------------

	public ObjShape getNPCshape() { return npcShape; }
	public TextureImage getNPCtexture() { return npcTx; }


	// ---------- NETWORKING SECTION ----------------

	public ObjShape getGhostShape() { return ghostS; }
	public TextureImage getGhostTexture() { return ghostTx; }
	public GhostManager getGhostManager() { return gm; }
	public Engine getEngine() { return engine; }
	public ProtocolClient getProtClient() {return protClient; }
	
	private void setupNetworking() {
		isClientConnected = false;	
		try{
			protClient = new ProtocolClient(InetAddress.getByName(serverAddress), serverPort, serverProtocol, this);
		}catch (UnknownHostException e){
			e.printStackTrace();
		}catch (IOException e){
			e.printStackTrace();
		}

		if (protClient == null){
			System.out.println("missing protocol host");
		}else{
			// Send the initial join message with a unique identifier for this client
			System.out.println("sending join message to protocol host");
			protClient.sendJoinMessage();
		}
	}
	
	protected void processNetworking(float elapsTime){
		// Process packets received by the client from the server
		if (protClient != null){
			protClient.processPackets();
		}
	}

	public Vector3f getPlayerPosition() {
		return avatar.getWorldLocation();
	}

	public void setIsConnected(boolean value) {
		this.isClientConnected = value;
	}
	
	private class SendCloseConnectionPacketAction extends AbstractInputAction
	{	@Override
		public void performAction(float time, net.java.games.input.Event evt) 
		{	if(protClient != null && isClientConnected == true)
			{	protClient.sendByeMessage();
			}
		}
	}

	public void setLineStatus(){
		// if(showLines){
		// 	showLines = false;
		// 	x.getRenderStates().disableRendering();
		// 	y.getRenderStates().disableRendering();
		// 	z.getRenderStates().disableRendering();
		// }else{
		// 	showLines = true;
		// 	x.getRenderStates().enableRendering();
		// 	y.getRenderStates().enableRendering();
		// 	z.getRenderStates().enableRendering();
		// }
	}

}