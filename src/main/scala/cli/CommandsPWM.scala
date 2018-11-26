package cli
import com.monovore.decline._

object CommandsPWM extends CommandsPWM
trait CommandsPWM extends InsertCommands with ConsensusCommands {


  lazy val mainCommand: Opts[Unit] = listSubcommand orElse mergeSubcommand orElse manualInsertSubcommand orElse insertSubcommand orElse consensusSubcommand orElse randomSubcommand
}