import better.files.File
import pwms.{LoaderPWM, PWM}

/*
//val repeats = List(20, 40, 100)
val r_str = repeats.map(i => s"--max_repeats $i").mkString(" ")
val tries = 100
val retries = 1000
val name = "/data/test/text_" + repeats.mkString("_") + s"retries_${retries}_tries_${tries}.fa"
*/
//val str = s"""docker run -v /data:/data quay.io/comp-bio-aging/pwm:0.0.16 repeated_generate --avoid BsaI --avoid BsmBI --avoid BtgZI --retries ${retries} --tries ${tries} ${r_str} /data/sources/horseq/Data-HOR-out/PWMs/AntonWorkflow/InsertedPWMs/CombinatorialPWM/Ins_2-Alin_2-prohibit-canon_chr21-CD.csv ${name}"""
val version = "0.0.17"
val doc = "docker run -v /data:/data quay.io/comp-bio-aging/pwm:${version}"
val base = "/data/sources/horseq/Data-HOR-out/PWMs/AntonWorkflow/InsertedPWMs/CombinatorialPWM/"
val sample = base + "Ins_2-Alin_2-prohibit-canon_chr1-JK_mul29.csv"
/*
val repeats = 40
val str = s"run generate_analytical --max_repeats ${repeats} --tries 10000 ${sample} /data/test/${version}.fa"
println("\n\n\n")
println(str)
println("\n\n\n")
//23 25 24
//NO_14
*/
println(LoaderPWM.loadFile(File(sample)).matrix.cols)