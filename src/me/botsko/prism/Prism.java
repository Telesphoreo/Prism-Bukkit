package me.botsko.prism;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;

import me.botsko.prism.actionlibs.ActionRecorder;
import me.botsko.prism.actionlibs.ActionsQuery;
import me.botsko.prism.actiontypes.ActionType;
import me.botsko.prism.actiontypes.BlockBreakType;
import me.botsko.prism.actiontypes.BlockBurnType;
import me.botsko.prism.actiontypes.BlockFadeType;
import me.botsko.prism.actiontypes.BlockFormType;
import me.botsko.prism.actiontypes.BlockPlaceType;
import me.botsko.prism.actiontypes.EndermanPickupType;
import me.botsko.prism.actiontypes.EndermanPlaceType;
import me.botsko.prism.actiontypes.EntityExplodeType;
import me.botsko.prism.actiontypes.EntityKillType;
import me.botsko.prism.actiontypes.FireballType;
import me.botsko.prism.actiontypes.FlintSteelType;
import me.botsko.prism.actiontypes.GenericActionType;
import me.botsko.prism.actiontypes.LavaBucketType;
import me.botsko.prism.actiontypes.LavaIgniteType;
import me.botsko.prism.actiontypes.LeafDecayType;
import me.botsko.prism.actiontypes.LightningType;
import me.botsko.prism.actiontypes.SheepEatType;
import me.botsko.prism.actiontypes.WaterBucketType;
import me.botsko.prism.appliers.PreviewSession;
import me.botsko.prism.commands.PrismCommandExecutor;
import me.botsko.prism.db.Mysql;
import me.botsko.prism.listeners.PrismBlockEvents;
import me.botsko.prism.listeners.PrismEntityEvents;
import me.botsko.prism.listeners.PrismPlayerInteractEvent;
import me.botsko.prism.listeners.PrismWorldEvents;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandExecutor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class Prism extends JavaPlugin {

	protected String msg_name = "Prism";
	protected Logger log = Logger.getLogger("Minecraft");
	public FileConfiguration config;
	protected Language language;
	public Connection conn = null;
	
	public HashMap<String,ActionType> actionTypes = new HashMap<String,ActionType>();
	public ActionRecorder actionsRecorder;
	public ActionsQuery actionsQuery;
	public ArrayList<String> playersWithActiveTools = new ArrayList<String>();
	public HashMap<String,PreviewSession> playerActivePreviews = new HashMap<String,PreviewSession>();
	
	
    /**
     * Enables the plugin and activates our player listeners
     */
	@Override
	public void onEnable(){
		
		this.log("Initializing plugin. By Viveleroi (and team), Darkhelmet Minecraft: s.dhmc.us");
		
//		try {
//		    Metrics metrics = new Metrics(this);
//		    metrics.start();
//		} catch (IOException e) {
//		    log("MCStats submission failed.");
//		}
		
		// Load configuration, or install if new
		loadConfig();
		
		// Assign event listeners
		getServer().getPluginManager().registerEvents(new PrismBlockEvents( this ), this);
		getServer().getPluginManager().registerEvents(new PrismEntityEvents( this ), this);
		getServer().getPluginManager().registerEvents(new PrismWorldEvents( this ), this);
		getServer().getPluginManager().registerEvents(new PrismPlayerInteractEvent( this ), this);
		
		// Add commands
		getCommand("prism").setExecutor( (CommandExecutor) new PrismCommandExecutor(this) );
		
		// Register all known action types
		registerActionTypes();
		
		// Init re-used classes
		actionsRecorder = new ActionRecorder(this);
		actionsQuery = new ActionsQuery(this);
		
	}
	
	
	/**
	 * Load configuration and language files
	 */
	public void loadConfig(){
		PrismConfig mc = new PrismConfig( this );
		config = mc.getConfig();
		// Load language files
		language = new Language( this, mc.getLang() );
	}
	
	
	/**
     * Setup a generic connection all non-scheduled methods may share
	 * @throws ClassNotFoundException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 * @return true if we successfully connected to the db.
     */
	public void dbc(){
		Mysql mysql = new Mysql(
				config.getString("prism.mysql.username"), 
				config.getString("prism.mysql.password"), 
				config.getString("prism.mysql.hostname"), 
				config.getString("prism.mysql.database"), 
				config.getString("prism.mysql.port")
		);
		conn = mysql.getConn();
	}
	
	
	/**
	 * 
	 * @return
	 */
	public Language getLang(){
		return this.language;
	}
	
	
	/**
	 * 
	 */
	protected void registerActionTypes(){
		
		actionTypes.put("block-break", new BlockBreakType());
		actionTypes.put("block-burn", new BlockBurnType());
		actionTypes.put("block-fade", new BlockFadeType());
		actionTypes.put("block-form", new BlockFormType());
		actionTypes.put("block-place", new BlockPlaceType());
		actionTypes.put("enderman-pickup", new EndermanPickupType());
		actionTypes.put("enderman-place", new EndermanPlaceType());
		actionTypes.put("entity-explode", new EntityExplodeType());
		actionTypes.put("entity-kill", new EntityKillType());
		actionTypes.put("flint-steel", new FlintSteelType());
		actionTypes.put("fireball", new FireballType());
		actionTypes.put("lava-bucket", new LavaBucketType());
		actionTypes.put("lava-ignite", new LavaIgniteType());
		actionTypes.put("leaf-decay", new LeafDecayType());
		actionTypes.put("lightning", new LightningType());
		actionTypes.put("sheep-eat", new SheepEatType());
		actionTypes.put("water-bucket", new WaterBucketType());
		
	}
	
	
	/**
	 * Pulls either a generic or a specific action type
	 * for assignment to a block, entity, etc action.
	 * 
	 * @todo Might wanna move this to a registry class
	 * @param type
	 * @return
	 */
	public ActionType getActionType(String type){
		ActionType actionType = new GenericActionType();
		if( actionTypes.containsKey(type) ){
			actionType = actionTypes.get(type);
		}
		return actionType;
	}
	
	
	/**
	 * 
	 * @param msg
	 * @return
	 */
	public String playerHeaderMsg(String msg){
		if(msg != null){
			return ChatColor.LIGHT_PURPLE + msg_name+" // " + ChatColor.WHITE + msg;
		}
		return "";
	}
	
	
	/**
	 * 
	 * @param msg
	 * @return
	 */
	public String playerMsg(String msg){
		if(msg != null){
			return ChatColor.WHITE + msg;
		}
		return "";
	}
	
	
	/**
	 * 
	 * @param player
	 * @param cmd
	 * @param help
	 */
	public String playerHelp( String cmd, String help ){
		return ChatColor.LIGHT_PURPLE + "/" + cmd + ": " + ChatColor.WHITE + help;
	}
	
	
	/**
	 * 
	 * @param msg
	 * @return
	 */
	public String playerError(String msg){
		if(msg != null){
			return ChatColor.LIGHT_PURPLE + msg_name+" // " + ChatColor.RED + msg;
		}
		return "";
	}
	
	
	/**
	 * 
	 * @param msg
	 * @return
	 */
	public String msgNoPermission(){
		return playerError("You don't have permission to perform this action.");
	}
	
	
	/**
	 * 
	 * @param message
	 */
	public void log(String message){
		log.info("["+msg_name+"]: " + message);
	}
	
	
	/**
	 * 
	 * @param message
	 */
	public void debug(String message){
		if(this.config.getBoolean("prism.debug")){
			log.info("["+msg_name+"]: " + message);
		}
	}
	
	
	/**
	 * Shutdown
	 */
	@Override
	public void onDisable(){
		this.log("Closing plugin.");
	}
}
