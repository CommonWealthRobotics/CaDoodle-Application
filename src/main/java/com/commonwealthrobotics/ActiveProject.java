package com.commonwealthrobotics;

import java.io.File;
import java.io.IOException;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.TransportException;

import com.neuronrobotics.bowlerstudio.assets.ConfigurationDatabase;
import com.neuronrobotics.bowlerstudio.scripting.ScriptingEngine;
import com.neuronrobotics.bowlerstudio.scripting.cadoodle.CaDoodleFile;

public class ActiveProject {
	static String[] adjectives = {
		    "Effervescent", "Zestful", "Vivacious", "Ebullient", "Sprightly",
		    "Exuberant", "Jocund", "Mirthful", "Zippy", "Gleeful",
		    "Buoyant", "Chipper", "Peppy", "Perky", "Jaunty",
		    "Blithe", "Joyous", "Spirited", "Vibrant", "Lively",
		    "Zealous", "Jubilant", "Merry", "Elated", "Euphoric",
		    "Bubbly", "Chirpy", "Animated", "Bouncy", "Energetic",
		    "Frisky", "Sparkling", "Vivid", "Zappy", "Zippy",
		    "Snappy", "Peppy", "Effulgent", "Radiant", "Luminous",
		    "Beaming", "Glowing", "Incandescent", "Resplendent", "Lustrous",
		    "Glistening", "Scintillating", "Efflorescent", "Blooming", "Flourishing",
		    "Thriving", "Burgeoning", "Prolific", "Fecund", "Teeming",
		    "Abundant", "Plentiful", "Bountiful", "Copious", "Profuse",
		    "Exuberant", "Luxuriant", "Lush", "Verdant", "Fertile",
		    "Productive", "Fruitful", "Generative", "Creative", "Imaginative",
		    "Inventive", "Ingenious", "Clever", "Brilliant", "Dazzling",
		    "Bright", "Luminous", "Radiant", "Gleaming", "Glittering",
		    "Sparkling", "Twinkling", "Shimmering", "Glowing", "Beaming",
		    "Effulgent", "Refulgent", "Resplendent", "Splendid", "Magnificent",
		    "Majestic", "Grand", "Glorious", "Superb", "Marvelous",
		    "Wonderful", "Fantastic", "Fabulous", "Astounding", "Amazing"
		};

	static String[] creaturesMachines = {
		    "Dragon", "Robot", "Unicorn", "Cyborg", "Griffin",
		    "Automaton", "Phoenix", "Mech", "Kraken", "Droid",
		    "Chimera", "Golem", "Pegasus", "Android", "Hydra",
		    "Centaur", "Drone", "Sphinx", "Exosuit", "Minotaur",
		    "Cyclops", "Hologram", "Werewolf", "Nanobot", "Gorgon",
		    "Replicant", "Banshee", "Hovercraft", "Basilisk", "Jetpack",
		    "Gargoyle", "Teleporter", "Manticore", "Forcefield",
		    "Submarine", "Cerberus", "Hoverboard", "Siren", "Skycycle",
		    "Yeti", "Hoverbike", "Sasquatch", "Hyperloop", "Thunderbird",
		    "Gyrocopter", "Leviathan", "Airship", "Behemoth", "Starship",
		    "Colossus", "Monowheel", "Titan", "Rocketship", "Gremlin",
		    "Hovercar", "Imp", "Zeppelin", "Ogre", "Monorail",
		    "Troll", "Bathyscaphe", "Gnome", "Segway", "Leprechaun",
		    "Hyperpod", "Fairy", "Gyrocar", "Elf", "Ornithopter",
		    "Wraith", "Hoversuit", "Specter", "Levitator", "Phantom",
		    "Telekinetic", "Goblin", "Gravicycle", "Dwarf",
		     "Jetbike", "Hobgoblin", "Ekranoplan", 
		    "Aerotrain", "Sprite", "Maglev",  "Aerosled",
		     "Cybertooth", "Ifrit", "Warpod", "Elemental",
		    "Hoversled", "Goliath", "Warpcraft", "Juggernaut", "Vortexer"
		};
	private static int index = ((Double) ConfigurationDatabase.get(
			"CaDoodle", 
			"projectNameIndex", 
			(Math.random()*creaturesMachines.length))).intValue();
	

	public static String getNextRandomName() {
		index++;
		if(index>=creaturesMachines.length)
			index=0;
		ConfigurationDatabase.put("CaDoodle", "projectNameIndex", index);
		return adjectives[(int) (Math.random()*adjectives.length)]+"_"+creaturesMachines[index];
	}
	public File getActiveProject() {
		try {
			return (File)ConfigurationDatabase.get(
					"CaDoodle", 
					"acriveFile", 
					ScriptingEngine.fileFromGit("", ""));
		} catch (InvalidRemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TransportException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (GitAPIException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		File random = new File(getNextRandomName()+".doodle");
		ConfigurationDatabase.put("CaDoodle", "acriveFile", random);
		return random;
	}
	public CaDoodleFile loadActive() throws Exception {
		return CaDoodleFile.fromFile(getActiveProject());
	}
	public void save(CaDoodleFile cf) {
		cf.setSelf(getActiveProject());
		
		try {
			cf.save();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
