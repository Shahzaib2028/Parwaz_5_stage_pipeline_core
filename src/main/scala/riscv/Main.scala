package riscv
import chisel3._
class Main extends Module{
  val io = IO(new Bundle{
    //val input = Output(UInt(32.W))
    //val pc = Output(UInt(32.W))
    val instruction = Output(UInt(32.W))
    val AluOut = Output(SInt(32.W))
    val branchCheck = Output(UInt(1.W))
  })
  val control = Module(new Control)
  val imm = Module(new immGen)
  val aluCtrl = Module(new AluControl)
  val alu = Module(new Alu)
  val reg = Module(new registerFile)
  val InsMem = Module(new InsMem)
  val PC = Module(new PC)
  val jalr = Module(new JalrTarget)
  val dataMem = Module(new DataMem)
  val if_pipe = Module(new IF_Pipeline())
  val id_pipe = Module(new ID_Pipeline())
  val exe_pipe = Module(new Ex_Pipeline())
  val mem_pipe = Module(new MEM_Piepline())
  val forwarding = Module(new Forwarding())
  val branch = Module(new branchUnit())
  val hazard_unit = Module(new HazardDetect())
  val branchForward = Module(new BranchForwarding())
  //val structural = Module(new StructuralHazard())



	
  //Pc Connection
  PC.io.input := PC.io.pc4
  //io.pc := PC.io.pc(11,2)

  //Instruction Memory Connection
  InsMem.io.wrAddr := PC.io.pc(11,2)
  io.instruction := InsMem.io.rdData

  //IF_ID----in----
  if_pipe.io.pc_in := PC.io.pc
  if_pipe.io.pc4_in := PC.io.pc4
  if_pipe.io.ins_in := io.instruction

  //-----------ID------------------
  //Control Connection
  control.io.opcode := if_pipe.io.ins_out(6,0) //InsMem.io.rdData(6,0)

  //Immediate Generation Connection
  imm.io.ins := if_pipe.io.ins_out
  imm.io.pc :=  if_pipe.io.pc_out

  //RegisterFile Connection
  reg.io.rs1_sel := if_pipe.io.ins_out(19,15)
  reg.io.rs2_sel := if_pipe.io.ins_out(24,20)


  //Alu Control Connection
  aluCtrl.io.ALUop := control.io.aluOp
  aluCtrl.io.func3 := if_pipe.io.ins_out(14,12)
  aluCtrl.io.func7 := if_pipe.io.ins_out(30)

  //ALU Connection
  when(control.io.oprA === 0.U || control.io.oprA === 3.U){
    id_pipe.io.operandA_in := reg.io.rs1
  }.elsewhen(control.io.oprA === 2.U){
    id_pipe.io.operandA_in := if_pipe.io.pc4_out.asSInt //+ 4.U).asSInt
  }.elsewhen(control.io.oprA === 1.U){ ///////////
    id_pipe.io.operandA_in := if_pipe.io.pc_out.asSInt
  }.otherwise{
    id_pipe.io.operandA_in := DontCare //alu.io.a
  }

  when(control.io.oprB === 0.U){
    id_pipe.io.operandB_in := reg.io.rs2
  }.otherwise{
    when(id_pipe.io.ExtendSel_Out === 0.U){
      id_pipe.io.operandB_in := imm.io.i
    }.elsewhen(id_pipe.io.ExtendSel_Out === 2.U){
      id_pipe.io.operandB_in := imm.io.S
    }.elsewhen(id_pipe.io.ExtendSel_Out === 1.U){
      id_pipe.io.operandB_in := imm.io.u
    }.otherwise{
      id_pipe.io.operandB_in := reg.io.rs2 //DontCare
    }
  }

  //ID_EXE------in--------


  id_pipe.io.opr_A_sel_in := control.io.oprA
  id_pipe.io.opr_B_sel_in := control.io.oprB
  id_pipe.io.rs1_sel_in := if_pipe.io.ins_out(19,15)
  id_pipe.io.rs2_sel_in := if_pipe.io.ins_out(24,20)
  id_pipe.io.memWrite_in := control.io.memWrite
  id_pipe.io.memRead_in := control.io.memRead
  id_pipe.io.memToReg_in := control.io.memToReg
  id_pipe.io.rd_in := if_pipe.io.ins_out(11,7)
  id_pipe.io.strData_in := reg.io.rs2
  id_pipe.io.aluCtrl_in := aluCtrl.io.out
  id_pipe.io.regWrite_in := control.io.regWrite
  id_pipe.io.branch_In := control.io.branch
  id_pipe.io.Aluop_In := control.io.aluOp
  id_pipe.io.NextPcSel_In := control.io.nextPcSel
  id_pipe.io.ExtendSel_In := control.io.extendSel
  //ID_EXE------in--------

  //---------------Branching------------

	branch.io.rs1 := reg.io.rs1
	branch.io.rs2 := reg.io.rs2
	branch.io.func3 := if_pipe.io.ins_out(14,12)
	
  //---------------Branching------------



	//structural.io.rs1_sel := if_pipe.io.ins_out(19,15)
	//structural.io.rs2_sel := if_pipe.io.ins_out(24,20)
	//structural.io.MEM_WR_RegisterWrite := mem_pipe.io.regWrite_out
	//structural.io.MEM_WR_rd := mem_pipe.io.rd_out

	
	//when(structural.io.forward_rs1 === 1.U){
	//	id_pipe.io.operandA_in := reg.io.writeData
	//}.otherwise{
	//	id_pipe.io.operandA_in := reg.io.rs1
	//}


	//when(structural.io.forward_rs2 === 1.U){
	//	id_pipe.io.operandB_in := reg.io.writeData
	//}.otherwise{
	//	id_pipe.io.operandB_in := reg.io.rs2
	//}


//-------------------------STALLING-----------------------------
	
	hazard_unit.io.ID_EX_rdregister_In := id_pipe.io.rd_out
	hazard_unit.io.IF_ID_instruction_In := if_pipe.io.ins_out
	hazard_unit.io.ID_EX_memRead_In := id_pipe.io.memRead_out
	hazard_unit.io.pc_In := if_pipe.io.pc4_out
	hazard_unit.io.curr_pc_In := if_pipe.io.pc_out



	when(hazard_unit.io.ins_forward === "b1".U){
		if_pipe.io.ins_in := hazard_unit.io.IF_ID_instruction_Out
		if_pipe.io.pc_in := hazard_unit.io.curr_pc_Out
	}.otherwise{
		if_pipe.io.ins_in := InsMem.io.rdData
	}


	when(hazard_unit.io.pc_forward === "b1".U){
		PC.io.input := hazard_unit.io.pc_Out
	}.otherwise{
		when(control.io.nextPcSel === "b01".U){
			when(control.io.branch === 1.U && branch.io.output === 1.U){
				PC.io.input  := imm.io.sb.asUInt
				if_pipe.io.pc_in := 0.U
				if_pipe.io.pc4_in := 0.U
				if_pipe.io.ins_in := 0.U
			}.otherwise{
				PC.io.input := PC.io.pc4
			}
		}.elsewhen(control.io.nextPcSel === "b10".U){
			PC.io.input  := imm.io.uj.asUInt
			if_pipe.io.pc_in := 0.U
			if_pipe.io.pc4_in := 0.U
			if_pipe.io.ins_in := 0.U
		}.elsewhen(control.io.nextPcSel === "b11".U){
			PC.io.input  := jalr.io.out.asUInt
			if_pipe.io.pc_in := 0.U
			if_pipe.io.pc4_in := 0.U
			if_pipe.io.ins_in := 0.U
		}.otherwise{
			PC.io.input := PC.io.pc4
		}
	}

	when(hazard_unit.io.out === 1.U){
		id_pipe.io.memWrite_in := 0.U
		id_pipe.io.memRead_in := 0.U
		id_pipe.io.memToReg_in := 0.U
		id_pipe.io.regWrite_in := 0.U
		id_pipe.io.NextPcSel_In := 0.U
		id_pipe.io.opr_A_sel_in := 0.U
		id_pipe.io.opr_A_sel_in := 0.U
		id_pipe.io.branch_In := 0.U
		id_pipe.io.Aluop_In := 0.U
		PC.io.input := PC.io.pc4
		
	}.otherwise{
		id_pipe.io.opr_A_sel_in := control.io.oprA
  		id_pipe.io.opr_B_sel_in := control.io.oprB
  		id_pipe.io.memWrite_in := control.io.memWrite
  		id_pipe.io.memRead_in := control.io.memRead
  		id_pipe.io.memToReg_in := control.io.memToReg
  		id_pipe.io.regWrite_in := control.io.regWrite
  		id_pipe.io.branch_In := control.io.branch
  		id_pipe.io.Aluop_In := control.io.aluOp
  		id_pipe.io.NextPcSel_In := control.io.nextPcSel
	}

	//when(control.io.branch === 1.U){
	//	when(branch.io.output === 1.U){
	//		PC.io.input := imm.io.sb.asUInt
	//		if_pipe.io.ins_in := 0.U
	//	}.otherwise{
	//		PC.io.input := PC.io.pc4
	//	}
	//}

	



  //-------------------EXE----------------


  alu.io.aluControl := id_pipe.io.aluCtrl_out
  alu.io.a := id_pipe.io.operandA_out
  alu.io.b := id_pipe.io.operandB_out
  exe_pipe.io.alu_Output_input := alu.io.aluOut
  exe_pipe.io.alu_branch_output_input := alu.io.branch
  io.AluOut := exe_pipe.io.alu_Output_output //alu.io.aluOut
  io.branchCheck := exe_pipe.io.alu_branch_output_output //alu.io.branch

  //EXE_MEM------in------
  exe_pipe.io.rs2_in := id_pipe.io.operandB_out
  exe_pipe.io.rs1_in := id_pipe.io.operandA_out
  exe_pipe.io.rs1_sel_in := id_pipe.io.rs1_sel_Out
  exe_pipe.io.rs2_sel_in := id_pipe.io.rs2_sel_Out
  exe_pipe.io.memWrite_in := id_pipe.io.memWrite_out
  exe_pipe.io.memRead_in := id_pipe.io.memRead_out
  exe_pipe.io.memToReg_in := id_pipe.io.memToReg_out
  exe_pipe.io.rd_in := id_pipe.io.rd_out
  exe_pipe.io.strData_in := exe_pipe.io.rs2_out
  exe_pipe.io.alu_Output_input := alu.io.aluOut
  exe_pipe.io.regWrite_in := id_pipe.io.regWrite_out




  //Data Memory Connection
  dataMem.io.store := exe_pipe.io.memWrite_out
  dataMem.io.load := exe_pipe.io.memRead_out
  dataMem.io.addrr := exe_pipe.io.alu_Output_output(9,2).asUInt
  dataMem.io.storedata := exe_pipe.io.rs2_out //reg.io.rs2
  /**when(control.io.memToReg === 0.U){
    reg.io.writeData := io.AluOut
  }.elsewhen(control.io.memToReg === 1.U){
    reg.io.writeData := dataMem.io.dataOut
  }.otherwise{
    reg.io.writeData := dataMem.io.dataOut
  }**/

  //MEM_WR----------in-------

  mem_pipe.io.rs1_sel_in := exe_pipe.io.rs1_sel_Out
  mem_pipe.io.rs2_sel_in := exe_pipe.io.rs2_sel_Out
  mem_pipe.io.memToReg_in := exe_pipe.io.memToReg_out
  mem_pipe.io.rd_in := exe_pipe.io.rd_out
  mem_pipe.io.aluOutput_in := exe_pipe.io.alu_Output_output
  mem_pipe.io.dataOut_in := dataMem.io.dataOut
  mem_pipe.io.regWrite_in := exe_pipe.io.regWrite_out
  mem_pipe.io.memRead_in := exe_pipe.io.memRead_out


  //when(control.io.memToReg === 1.U){
  //reg.io.writeData := io.AluOut
  //}.otherwise{
  //  reg.io.writeData := DontCare
  //}

  reg.io.rd_sel := mem_pipe.io.rd_out
  reg.io.regWrite := mem_pipe.io.regWrite_out

  //reg.io.rd_sel := mem_pipe.io.rd_out
  //reg.io.regWrite := mem_pipe.io.regWrite_out

  when(mem_pipe.io.memToReg_out === 1.U){
    reg.io.writeData :=  mem_pipe.io.dataOut_out
  }.otherwise{
     reg.io.writeData := mem_pipe.io.aluOutput_out
  }


//--------------------FORWARDIND HAZARD-------------------------
  forwarding.io.exe_pipe_regWrite_out := exe_pipe.io.regWrite_out
  forwarding.io.exe_pipe_rd_out := exe_pipe.io.rd_out
  forwarding.io.mem_pipe_regWrite_out := mem_pipe.io.regWrite_out
  forwarding.io.mem_pipe_rd_out := mem_pipe.io.rd_out
  forwarding.io.id_pipe_rs1_sel_out := id_pipe.io.rs1_sel_Out
  forwarding.io.id_pipe_rs2_sel_out := id_pipe.io.rs2_sel_Out


  when(id_pipe.io.opr_A_sel_Out === "b10".U){
    alu.io.a := id_pipe.io.operandA_out
  }.otherwise{
    when(forwarding.io.alu_A === "b00".U){
      alu.io.a := id_pipe.io.operandA_out
    }.elsewhen(forwarding.io.alu_A === "b01".U){
      alu.io.a := exe_pipe.io.alu_Output_output
    }.elsewhen(forwarding.io.alu_A === "b10".U){
      alu.io.a := reg.io.writeData
    }.otherwise{
      alu.io.a := id_pipe.io.operandA_out
    }
  }

  when(id_pipe.io.opr_B_sel_Out === "b1".U){
    alu.io.b := id_pipe.io.operandB_out
    when(forwarding.io.alu_B === "b00".U) {
      exe_pipe.io.rs2_in := id_pipe.io.operandB_out
    }.elsewhen(forwarding.io.alu_B === "b01".U) {
      exe_pipe.io.rs2_in := exe_pipe.io.alu_Output_output
    }.elsewhen(forwarding.io.alu_B === "b10".U) {
      exe_pipe.io.rs2_in := reg.io.writeData
    }.otherwise {
      exe_pipe.io.rs2_in := id_pipe.io.operandB_out
    }
  }.otherwise{
    when(forwarding.io.alu_B === "b00".U) {
      alu.io.b := id_pipe.io.operandB_out
      exe_pipe.io.rs2_in := id_pipe.io.operandB_out
    }.elsewhen(forwarding.io.alu_B === "b01".U) {
      alu.io.b := exe_pipe.io.alu_Output_output
      exe_pipe.io.rs2_in := exe_pipe.io.alu_Output_output
    }.elsewhen(forwarding.io.alu_B === "b10".U) {
      alu.io.b := reg.io.writeData
      exe_pipe.io.rs2_in := reg.io.writeData
    }.otherwise {
      alu.io.b := id_pipe.io.operandB_out
      exe_pipe.io.rs2_in := id_pipe.io.operandB_out
    }
  }

//--------------------FORWARDIND HAZARD-------------------------



//---------------------Branch Forwarding------------------------


	// FOR REGISTER RS1 in BRANCH FORWARDING

	branchForward.io.ID_rd := id_pipe.io.rd_out
	branchForward.io.ID_MemRead := id_pipe.io.memRead_out
	branchForward.io.EX_rd := exe_pipe.io.rd_out
	branchForward.io.EX_MemRead := exe_pipe.io.memRead_out
	branchForward.io.MEM_rd := mem_pipe.io.rd_out
	branchForward.io.MEM_MemRead := mem_pipe.io.memRead_out
	branchForward.io.rs1_sel := if_pipe.io.ins_out(19,15)
  	branchForward.io.rs2_sel := if_pipe.io.ins_out(24,20)
	branchForward.io.control_branch := control.io.branch

	when(branchForward.io.forward_a === "b0000".U) {
      
      		branch.io.rs1 := reg.io.rs1
      		jalr.io.rs1 := reg.io.rs1
    	} .elsewhen(branchForward.io.forward_a === "b0001".U) {
      
      		branch.io.rs1 := alu.io.aluOut
      		jalr.io.rs1 := reg.io.rs1
    	} .elsewhen(branchForward.io.forward_a === "b0010".U) {
      
      		branch.io.rs1 := exe_pipe.io.alu_Output_output
      		jalr.io.rs1 := reg.io.rs1
    	} .elsewhen(branchForward.io.forward_a === "b0011".U) {
      
      		branch.io.rs1 := reg.io.writeData
      		jalr.io.rs1 := reg.io.rs1
    	} .elsewhen(branchForward.io.forward_a === "b0100".U) {
     
      		branch.io.rs1 := dataMem.io.dataOut
      		jalr.io.rs1 := reg.io.rs1
    	} .elsewhen(branchForward.io.forward_a === "b0101".U) {
   
      		branch.io.rs1 := reg.io.writeData
      		jalr.io.rs1 := reg.io.rs1
    	}.elsewhen(branchForward.io.forward_a === "b0110".U) {
        
        	jalr.io.rs1 := alu.io.aluOut
        	branch.io.rs1 := reg.io.rs1
    	} .elsewhen(branchForward.io.forward_a === "b0111".U) {
        
        	jalr.io.rs1 := exe_pipe.io.alu_Output_output
        	branch.io.rs1 := reg.io.rs1
    	} .elsewhen(branchForward.io.forward_a === "b1000".U) {
        	jalr.io.rs1 := reg.io.writeData
        	branch.io.rs1 := reg.io.rs1
    	} .elsewhen(branchForward.io.forward_a === "b1001".U) {
        
        	jalr.io.rs1 := dataMem.io.dataOut
        	branch.io.rs1 := reg.io.rs1
    	} .elsewhen(branchForward.io.forward_a === "b1010".U) {
        	jalr.io.rs1 := reg.io.writeData
        	branch.io.rs1 := reg.io.rs1
    	}
      	.otherwise {
        	branch.io.rs1 := reg.io.rs1
        	jalr.io.rs1 := reg.io.rs1
    	}


	// FOR REGISTER RS2 in BRANCH FORWARDING
    	when(branchForward.io.forward_a === "b0000".U) {
      		branch.io.rs2 := reg.io.rs2
    	} .elsewhen(branchForward.io.forward_a === "b0001".U) {
      		branch.io.rs2 := alu.io.aluOut
    	} .elsewhen(branchForward.io.forward_a === "b0010".U) {
      		branch.io.rs2 := exe_pipe.io.alu_Output_output
    	} .elsewhen(branchForward.io.forward_a === "b0011".U) {
      		branch.io.rs2 := reg.io.writeData
    	} .elsewhen(branchForward.io.forward_a === "b0100".U) {
      		branch.io.rs2 := dataMem.io.dataOut
    	} .elsewhen(branchForward.io.forward_a === "b0101".U) {
      		branch.io.rs2 := reg.io.writeData
    	}.otherwise{
        	branch.io.rs2 := reg.io.rs2
      	}

    	jalr.io.imm := imm.io.i


	//--------------------STRUCTURAL HAZARD------------------

	
	

	when(branchForward.io.forward_b === "b00000".U){
		branch.io.rs2 := reg.io.rs2
	}.elsewhen(branchForward.io.forward_b === "b00001".U){
		branch.io.rs2 := alu.io.aluOut
	}.elsewhen(branchForward.io.forward_b === "b00010".U){
		branch.io.rs2 := exe_pipe.io.alu_Output_output
	}.elsewhen(branchForward.io.forward_b === "b00011".U){
		branch.io.rs2 := reg.io.writeData
	}.otherwise{
		branch.io.rs2 := reg.io.rs2
	}














	//Jalr Target Connection
  	//when(id_pipe.io.ExtendSel_Out === 0.U){
    	//	jalr.io.imm := imm.io.i
  	//}.elsewhen(id_pipe.io.ExtendSel_Out === 2.U){
    	//	jalr.io.imm := imm.io.S
  	//}.elsewhen(id_pipe.io.ExtendSel_Out === 1.U){
    	//	jalr.io.imm := imm.io.u
  	//}.otherwise{
    	//	jalr.io.imm := DontCare
  	//}
	//jalr.io.rs1 := reg.io.rs1




	//Data memory connections
		//datamem.io.store :=  control.io.MemWrite
		//datamem.io.load :=  control.io.MemRead
		//datamem.io.addr := (alu.io.out(9,2)).asUInt
		//datamem.io.store_data :=  register32.io.rs2


		//when(Execute.io.EX_MemToReg_Output === 0.U){
		//	register32.io.DataWrite := Execute.io.Ex_ALU_Output
		//}.elsewhen(Execute.io.EX_MemToReg_Output === 1.U){
		//	Mempipe.io.Ex_dataMem_dataOutput_Input := datamem.io.Data_Out
		//	register32.io.DataWrite := Mempipe.io.Ex_dataMem_dataOutput_Output	
		//}.otherwise{
		//	Mempipe.io.Ex_dataMem_dataOutput_Input := datamem.io.Data_Out
		//	register32.io.DataWrite := Mempipe.io.Ex_dataMem_dataOutput_Output
		//}
		//io.reg_output := register32.io.DataWrite

	//when((alu.io.branch.asUInt & ins_decode.io.branch_Output) === 1.U && ins_decode.io.Next_pc_sel_Output === 1.U){
		//	ins_fetch.io.pc_input  := immediate.io.Sb_Type.asUInt
		//}.elsewhen((alu.io.branch.asUInt & ins_decode.io.branch_Output) === 0.U && ins_decode.io.Next_pc_sel_Output === 1.U){
		//	ins_fetch.io.pc_input  := ins_fetch.io.pc_input + 4.U
		//}.elsewhen(ins_decode.io.Next_pc_sel_Output === 0.U){
		//	ins_fetch.io.pc_input  := ins_fetch.io.pc_input + 4.U
		//}.elsewhen(ins_decode.io.Next_pc_sel_Output === "b10".U){
		//	ins_fetch.io.pc_input := immediate.io.Uj_Type.asUInt
		//}.elsewhen(ins_decode.io.Next_pc_sel_Output === "b11".U){
		//	ins_fetch.io.pc_input := jalr.io.out.asUInt
		//}.otherwise{
		//	ins_fetch.io.pc_input := DontCare
		//}
				



		//--------------------Done-----------------------------
		//Register32 Connection
		//register32.io.rs1_sel := ins_fetch.io.ins_Output(19,15)
		//register32.io.rs2_sel := ins_fetch.io.ins_Output(24,20)
		//register32.io.rd_sel := ins_fetch.io.ins_Output(11,7)
		//register32.io.RegWrite := Execute.io.EX_RegWrite_Output
		//--------------------Done-------------------------------

		

		//-------------------Done-------------------------------
		//ALU CONTROL Connection
		//alu_control.io.ALUop := control.io.ALUoperation
		//alu_control.io.func3 := ins_fetch.io.ins_Output(14,12) //ins.io.rdData(14,12)
		//alu_control.io.func7 := ins_fetch.io.ins_Output(30) //ins.io.rdData(30)
		//-------------------Done-------------------------------


		//-----------------Done----------------------
		//OperandA connection
		
		//---------------Done-----------------------

		
		//alu.io.ALUcont := alu_control.io.ALUcont
		//register32.io.DataWrite := alu.io.out
		//io.reg_output := register32.io.rs1
		//io.reg_output := alu.io.out




}
