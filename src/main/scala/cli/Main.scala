package cli
import com.monovore.decline._


object Main extends CommandApp(
  name = "PWM",
  header = "PWM application",
  main = {
    CommandsPWM.mainCommand.map{ _=>
      //just to run it
    }
  }


)
