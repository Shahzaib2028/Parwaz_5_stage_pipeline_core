package riscv
import chisel3._
class branchUnit extends Module{
	val io = IO(new Bundle{
		val rs1 = Input(SInt(32.W))
		val rs2 = Input(SInt(32.W))
		val func3 = Input(UInt(3.W))
		val output = Output(UInt(1.W))


	})

	io.output := 0.U

	when(io.func3 === "b000".U) {
    		when(io.rs1 === io.rs2) {
      			io.output := 1.U
    		}.otherwise {
     			io.output := 0.U
		}
    	}.elsewhen(io.func3 === "b001".U){
		when(io.rs1 =/= io.rs2){
			io.output := 1.U
		}.otherwise{
			io.output := 0.U
		}	
	}.elsewhen(io.func3 === "b100".U){
		when(io.rs1 < io.rs2){
			io.output := 1.U
		}.otherwise{
			io.output := 0.U
		}
	}.elsewhen(io.func3 === "b101".U){
		when(io.rs1 >= io.rs2){
			io.output := 1.U
		}.otherwise{
			io.output := 0.U
		}
	}.elsewhen(io.func3 === "b110".U){
		when(io.rs1.asUInt < io.rs2.asUInt){
			io.output := 1.U
		}.otherwise{
			io.output := 0.U
		}
	}.elsewhen(io.func3 === "b111".U){
		when(io.rs1.asUInt >= io.rs2.asUInt){
			io.output := 1.U
		}.otherwise{
			io.output := 0.U
		}
	}.otherwise{
		io.output := 0.U
	}






}
