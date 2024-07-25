package com.commonwealthrobotics;

import com.neuronrobotics.bowlerstudio.assets.ConfigurationDatabase;

public class ActiveProject {
	private static String[] projectNames = {
		    "Robo_Buddy", "Space_Explorer", "Flower_Power", "Dino_World", "Super_Car",
		    "Dream_House", "Magic_Wand", "Flying_Saucer", "Underwater_City", "Tree_Fort",
		    "Rocket_Ship", "Fairy_Castle", "Dragons_Lair", "Pirate_Ship", "Time_Machine",
		    "Toy_Factory", "Jungle_Gym", "Butterfly_Garden", "Robot_Dog", "Candy_Land",
		    "Block_Village", "Ninja_Hideout", "Unicorn_Stable", "Brick_Tower", "Dinosaur_Park",
		    "Treehouse_City", "Alien_Spaceship", "Wizards_Tower", "Mermaid_Lagoon", "Race_Track",
		    "Superhero_Headquarters", "Princess_Palace", "Monster_Truck", "Enchanted_Forest", "Submarine",
		    "Haunted_House", "Roller_Coaster", "Ice_Palace", "Treasure_Island", "Futuristic_City",
		    "Volcano_Lair", "Magical_School", "Mars_Colony", "Jungle_Temple", "Dragons_Castle",
		    "Skate_Park", "Fairy_Garden", "Robot_Factory", "Pirate_Cove", "Moonbase",
		    "Dinosaur_Museum", "Floating_Island", "Secret_Lab", "Candy_Factory", "Ninja_Dojo",
		    "Underwater_Base", "Rocket_Launch_Pad", "Toy_Workshop", "Prehistoric_Park", "Alien_Zoo",
		    "Wizards_Library", "Mermaid_Kingdom", "Superhero_Academy", "Magical_Maze", "Steampunk_City",
		    "Dragons_Nest", "Fairy_Tale_Village", "Robot_Zoo", "Pirate_Bay", "Time_Travel_Station",
		    "Dino_Ranch", "Space_Station", "Enchanted_Castle", "Monster_Playground", "Unicorn_Cloud_Castle",
		    "Brick_Land", "Wizards_School", "Mermaids_Grotto", "Superhero_Training_Ground", "Princess_Treehouse",
		    "Alien_Planet", "Ninja_Training_Camp", "Candy_Mountain", "Robot_City", "Pirates_Fortress",
		    "Dino_Safari", "Fairy_Treehouse", "Magical_Circus", "Underwater_Kingdom", "Space_Colony",
		    "Dragons_Playground", "Toy_Town", "Jungle_Adventures", "Monster_Island", "Hover_League_Arena",
		    "Wizards_Garden", "Mermaids_Ship", "Superhero_City", "Princess_Garden", "Alien_Mothership"
		};
	private static int index = ((Double) ConfigurationDatabase.get("CaDoodle", "projectNameIndex", (Math.random()*projectNames.length))).intValue();
	
	public static String getCurrentRandomName() {
		return projectNames[index];
	}
	public static String getNextRandomName() {
		index++;
		if(index>=projectNames.length)
			index=0;
		ConfigurationDatabase.put("CaDoodle", "projectNameIndex", index);
		return projectNames[index];
	}
}
