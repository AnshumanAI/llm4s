import org.llm4s.imagegeneration._
import java.nio.file.Paths

object HuggingFaceTest {
  def main(args: Array[String]): Unit = {
    println("🤗 HuggingFace Image Generation Test")
    println("===================================")
    
    val hfToken = sys.env.get("HF_TOKEN") match {
      case Some(token) => token
      case None =>
        println("⚠️  Set HF_TOKEN environment variable")
        println("📝 Get free token: https://huggingface.co/settings/tokens")
        println("🔧 Windows: set HF_TOKEN=your_token")
        println("🧪 Using demo mode for now...")
        "demo_token"
    }
    
    val config = HuggingFaceConfig(apiKey = hfToken)
    val client = ImageGeneration.client(config)
    
    println("📋 Testing health...")
    client.health() match {
      case Right(status) => 
        println(s"✅ ${status.message}")
      case Left(error) =>
        println(s"❌ Health check failed: ${error.message}")
        if (hfToken == "demo_token") {
          println("💡 Expected - get real HF token to test")
          return
        }
    }
    
    println("🖼️ Generating image...")
    val prompt = "a cute cat sitting on a table, digital art"
    client.generateImage(prompt) match {
      case Right(image) =>
        println(s"✅ Generated image!")
        println(s"📏 Size: ${image.size.description}")
        
        val outputPath = Paths.get("hf_generated.png")
        image.saveToFile(outputPath) match {
          case Right(_) =>
            println(s"💾 Saved: ${outputPath.toAbsolutePath}")
            println("🎉 SUCCESS!")
          case Left(error) =>
            println(s"❌ Save failed: ${error.message}")
        }
        
      case Left(error) =>
        println(s"❌ Generation failed: ${error.message}")
    }
  }
} 