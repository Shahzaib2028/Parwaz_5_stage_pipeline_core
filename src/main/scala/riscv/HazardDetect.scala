package riscv

import chisel3._

class HazardDetect extends Module{
	val io = IO(new Bundle{
		val ID_EX_memRead_In = Input(UInt(1.W))
		val ID_EX_rdregister_In = Input(UInt(5.W))
		val IF_ID_instruction_In = Input(UInt(32.W))
		val pc_In = Input(UInt(32.W))
		val curr_pc_In = Input(UInt(32.W))


		val IF_ID_instruction_Out = Output(UInt(32.W))
		val pc_Out = Output(UInt(32.W))
		val curr_pc_Out = Output(UInt(32.W))
		val ins_forward = Output(UInt(1.W))
		val pc_forward = Output(UInt(1.W))
		val control_pins = Output(UInt(1.W))
		val out = Output(UInt(1.W))

	})

		val rs1_sel = io.IF_ID_instruction_In(19,15)
		val rs2_sel = io.IF_ID_instruction_In(24,20)

		when(io.ID_EX_memRead_In === "b1".U && ((io.ID_EX_rdregister_In === rs1_sel) || (io.ID_EX_rdregister_In === rs2_sel))){
			io.IF_ID_instruction_Out := io.IF_ID_instruction_In
			io.pc_Out := io.pc_In
			io.curr_pc_Out := io.curr_pc_In
			io.ins_forward := 1.U
			io.pc_forward := 1.U
			io.control_pins := 1.U
			io.out := 1.U
		}.otherwise{
			io.IF_ID_instruction_Out := io.IF_ID_instruction_In
			io.pc_Out := io.pc_In
			io.curr_pc_Out := io.curr_pc_In
			io.ins_forward := 0.U
			io.pc_forward := 0.U
			io.control_pins := 0.U
			io.out := 0.U
		}














}
