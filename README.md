# Objective Color Calibration (OCC)

This is a project done as a plugin for the [ImageJ](https://imagej.nih.gov/ij/) platform. 
It finds a particular pattern on a picture, calibrates perspective to make the pattern
perpendicular to the image, then tries to improve resolution and finally calibrates color.

## Publication

* Yargo V. Tessaro, Sérgio S. Furuie, Denise M. Nakamura,
  "Objective color calibration for manufacturing facial prostheses,"
  J. Biomed. Opt. 26(2), 025002 (2021), doi: 10.1117/1.JBO.26.2.025002.

* Yargo V. Tessaro, Sérgio S. Furuie,
  "Metodologia para Calibrar e Restaurar Imagens em Teledermatologia".
  Anais Do XXV CBEB - Edição 2016. p. 983-986
  
* Yargo V. Tessaro, Sérgio S. Furuie,
  "Abordagem para Melhorar e Avaliar a Qualidade de Imagens em Teledermatologia". 
  Anais do VII SIIM/VI SPS, 2015. p. 133-136.

## Installing

For a simple installation, simply download the
[latest relase](https://github.com/yargo13/correct_image/releases/)
and place it inside the `/plugins` folder of your
[ImageJ installation](https://imagej.nih.gov/ij/download.html)

## Executing

In progress...

## Contributing

In progress...

<!---
* In [Eclipse](http://eclipse.org), for example, it is as simple as
  _File&gt;Import...&gt;Existing Maven Project_.

* In [NetBeans](http://netbeans.org), it is even simpler:
  _File&gt;Open Project_.

* The same works in [IntelliJ](http://jetbrains.net).

* If [jEdit](http://jedit.org) is your preferred IDE, you will need the
  [Maven Plugin](http://plugins.jedit.org/plugins/?MavenPlugin).

Die-hard command-line developers can use Maven directly by calling `mvn`
in the project root.

However you build the project, in the end you will have the `.jar` file
(called *artifact* in Maven speak) in the `target/` subdirectory.

To copy the artifact into the correct place, you can call
`mvn -Dimagej.app.directory=/path/to/ImageJ.app/`.
This will not only copy your artifact, but also all the dependencies. Restart
your ImageJ or call *Help>Refresh Menus* to see your plugin in the menus.

Developing plugins in an IDE is convenient, especially for debugging. To
that end, the plugin contains a `main` method which sets the `plugins.dir`
system property (so that the plugin is added to the Plugins menu), starts
ImageJ, loads an image and runs the plugin. See also
[this page](https://imagej.net/Debugging#Debugging_plugins_in_an_IDE_.28Netbeans.2C_IntelliJ.2C_Eclipse.2C_etc.29)
for information how ImageJ makes it easier to debug in IDEs.

### Eclipse: To ensure that Maven copies the plugin to your ImageJ folder

1. Go to _Run Configurations..._
2. Choose _Maven Build_
3. Add the following parameter:
    - name: `imagej.app.directory`
    - value: `/path/to/ImageJ.app/`

This ensures that the final `.jar` file will also be copied to your ImageJ
plugins folder everytime you run the Maven Build

-->