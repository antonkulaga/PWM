package cli
import com.monovore.decline._

object CommandsPWM extends CommandsPWM
trait CommandsPWM extends GenerateCommands {

  lazy val mainCommand: Opts[Unit] = listSubcommand orElse
    mergeSubcommand orElse
    manualInsertSubcommand orElse
    insertSubcommand orElse
    consensusSubcommand orElse
    randomSubcommand orElse synthesisSubcommand orElse generateSubcommand orElse cloningSubcommand
}
