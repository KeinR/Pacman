del "C:\Users\orion\Documents\GitHub\Pacman\*.class" /s /q
set PATH_TO_FX="C:\Users\orion\Documents\javafx-sdk-12.0.1\lib"
javac --module-path %PATH_TO_FX% --add-modules javafx.controls net/keinr/pacman/Main.java
java --module-path %PATH_TO_FX% --add-modules javafx.controls net.keinr.pacman.Main
cmd.exe
