import java.awt.image.BufferedImage
import java.awt.{RenderingHints, Graphics2D, Font}
import javax.imageio.ImageIO


object AsciiArtTest {

  def test {
    val image = new BufferedImage(144, 32, BufferedImage.TYPE_INT_RGB);
    val g = image.getGraphics();
    g.setFont(new Font("Arial", Font.PLAIN, 18));
    val graphics = g.asInstanceOf[Graphics2D]
    graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
      RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    graphics.drawString("Hello World!", 6, 24);
    ImageIO.write(image, "png", new java.io.File("text.png"));

    for (y <- 0 until 32) {
      val sb = new StringBuilder();
      for (x <- 0 until 144)
        sb.append(
          if (image.getRGB(x, y) == -16777216) " " else if (image.getRGB(x, y) == -1) "-" else "|");
      System.out.println(sb);
    }
  }
  def main(args: Array[String]) {
    test
  }

}
