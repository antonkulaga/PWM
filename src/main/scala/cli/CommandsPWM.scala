package cli
import com.monovore.decline._

object CommandsPWM extends CommandsPWM
trait CommandsPWM extends AdvancedGenerateCommands {

  lazy val mainCommand: Opts[Unit] = listSubcommand orElse
    concatSubcommand orElse
    mergeSubcommand orElse
    manualInsertSubcommand orElse
    insertSubcommand orElse
    insertPWMSubcommand orElse
    consensusSubcommand orElse
    randomSubcommand orElse
    synthesisSubcommand orElse
    generateSubcommand orElse
    advancedGenerateSubcommand orElse
    cloningSubcommand
}
