
Source installation information for modders
-------------------------------------------
This code uses the NeoForge (net.neoforged.moddev) ModDevGradle buildscript. It
compiles directly against Mojang's official mappings, so there is no separate
"un-renamed"/SRG source step to worry about.

Setup Process:
==============================

Step 1: Open your command-line and browse to the folder where you extracted the zip file.

Step 2: Open the project in the IDE of your choice. The usual recommendation for an
IDE is either IntelliJ IDEA or Eclipse - simply open/import the repository folder and
let the Gradle project sync.

If at any point you are missing libraries in your IDE, or you've run into problems you can 
run `gradlew --refresh-dependencies` to refresh the local cache. `gradlew clean` to reset everything 
(this does not affect your code) and then start the process again.

Mapping Names:
=============================
By default, the MDK is configured to use the official mapping names from Mojang for methods and fields 
in the Minecraft codebase, supplemented with Parchment's community-sourced parameter names and javadoc.
These names are covered by a specific license. All modders should be aware of this license. For the
latest license text, refer to the mapping file itself, or the reference copy here:
https://github.com/NeoForged/NeoForm/blob/main/Mojang.md

Additional Resources: 
=========================
Community Documentation: https://docs.neoforged.net/
NeoForged Discord: https://discord.neoforged.net/
