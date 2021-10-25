package riscv
import chisel3._
class StructuralHazard extends Module {
  val io = IO(new Bundle {
    val rs1_sel = Input(UInt(5.W))
    val rs2_sel = Input(UInt(5.W))
    val MEM_WR_RegisterWrite = Input(UInt(1.W))
    val MEM_WR_rd = Input(UInt(5.W))
    val forward_rs1 = Output(UInt(1.W))
    val forward_rs2 = Output(UInt(1.W))
  })

  when(io.MEM_WR_RegisterWrite === 1.U &&  io.MEM_WR_rd === io.rs1_sel) {
    io.forward_rs1 := 1.U
  } .otherwise {
    io.forward_rs1 := 0.U
  }

  when(io.MEM_WR_RegisterWrite === 1.U && io.MEM_WR_rd === io.rs2_sel) {
    io.forward_rs2 := 1.U
  } .otherwise {
    io.forward_rs2 := 0.U
  }

}
