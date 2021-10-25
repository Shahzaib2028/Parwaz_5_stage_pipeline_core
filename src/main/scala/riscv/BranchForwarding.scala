package riscv
import chisel3._

class BranchForwarding extends Module {
  val io = IO(new Bundle {
    val ID_rd = Input(UInt(5.W))
    val ID_MemRead = Input(UInt(1.W))
    val EX_rd = Input(UInt(5.W))
    val EX_MemRead = Input(UInt(1.W))
    val MEM_rd = Input(UInt(5.W))
    val MEM_MemRead = Input(UInt(1.W))
    val rs1_sel = Input(UInt(5.W))
    val rs2_sel = Input(UInt(5.W))
    val control_branch = Input(UInt(1.W))
    val forward_a = Output(UInt(4.W))
    val forward_b = Output(UInt(4.W))
  })

    io.forward_a := "b0000".U
    io.forward_b := "b0000".U


    when(io.control_branch === 1.U) {
      // ALU Hazard
      when(io.ID_rd =/= "b00000".U && io.ID_MemRead =/= 1.U && (io.ID_rd === io.rs1_sel) && (io.ID_rd === io.rs2_sel)) {
        io.forward_a := "b0001".U
        io.forward_b := "b0001".U
      } .elsewhen(io.ID_rd =/= "b00000".U && io.ID_MemRead =/= 1.U && (io.ID_rd === io.rs1_sel)) {
        io.forward_a := "b0001".U
      } .elsewhen(io.ID_rd =/= "b00000".U && io.ID_MemRead =/= 1.U && (io.ID_rd === io.rs2_sel)) {
        io.forward_b := "b0001".U
      }

      // EX/MEM Hazard
      when(io.EX_rd =/= "b00000".U && io.EX_MemRead =/= 1.U &&
        ~((io.ID_rd =/= "b00000".U) && (io.ID_rd === io.rs1_sel) && (io.ID_rd === io.rs2_sel)) &&
        (io.EX_rd === io.rs1_sel) && (io.EX_rd === io.rs2_sel)) {

        io.forward_a := "b0010".U
        io.forward_b := "b0010".U

      } .elsewhen(io.EX_rd =/= "b00000".U && io.EX_MemRead =/= 1.U &&
        ~((io.ID_rd =/= "b00000".U) && (io.ID_rd === io.rs2_sel)) &&
        (io.EX_rd === io.rs2_sel)) {

        io.forward_b := "b0010".U

      } .elsewhen(io.EX_rd =/= "b00000".U && io.EX_MemRead =/= 1.U &&
        ~((io.ID_rd =/= "b00000".U) && (io.ID_rd === io.rs1_sel)) &&
        (io.EX_rd === io.rs1_sel)) {

        io.forward_a := "b0010".U

      } .elsewhen(io.EX_rd =/= "b00000".U && io.EX_MemRead === 1.U &&
        ~((io.ID_rd =/= "b00000".U) && (io.ID_rd === io.rs1_sel) && (io.ID_rd === io.rs2_sel)) &&
        (io.EX_rd === io.rs1_sel) && (io.EX_rd === io.rs2_sel)) {
        // FOR Load instructions
        io.forward_a := "b0100".U
        io.forward_b := "b0100".U

      } .elsewhen(io.EX_rd =/= "b00000".U && io.EX_MemRead === 1.U &&
        ~((io.ID_rd =/= "b00000".U) && (io.ID_rd === io.rs2_sel)) &&
        (io.EX_rd === io.rs2_sel)) {

        io.forward_b := "b0100".U

      } .elsewhen(io.control_branch === 1.U && io.EX_rd =/= "b00000".U && io.EX_MemRead === 1.U &&
        ~((io.ID_rd =/= "b00000".U) && (io.ID_rd === io.rs1_sel)) &&
        (io.EX_rd === io.rs1_sel)) {

        io.forward_a := "b0100".U

      }

      // MEM/WB Hazard
      when(io.MEM_rd =/= "b00000".U && io.MEM_MemRead =/= 1.U &&
        // IF NOT ALU HAZARD
        ~((io.ID_rd =/= "b00000".U) && (io.ID_rd === io.rs1_sel) && (io.ID_rd === io.rs2_sel)) &&
        // IF NOT EX/MEM HAZARD
        ~((io.EX_rd =/= "b00000".U) && (io.EX_rd === io.rs1_sel) && (io.EX_rd === io.rs2_sel)) &&
        (io.MEM_rd === io.rs1_sel) && (io.MEM_rd === io.rs2_sel)) {

        io.forward_a := "b0011".U
        io.forward_b := "b0011".U

      }
        .elsewhen(io.MEM_rd =/= "b00000".U && io.MEM_MemRead =/= 1.U &&
          // IF NOT ALU HAZARD
          ~((io.ID_rd =/= "b00000".U) && (io.ID_rd === io.rs2_sel)) &&
          // IF NOT EX/MEM HAZARD
          ~((io.EX_rd =/= "b00000".U) && (io.EX_rd === io.rs2_sel)) &&
          (io.MEM_rd === io.rs2_sel)) {

          io.forward_b := "b0011".U

        }
        .elsewhen(io.MEM_rd =/= "b00000".U && io.MEM_MemRead =/= 1.U &&
          // IF NOT ALU HAZARD
          ~((io.ID_rd =/= "b00000".U) && (io.ID_rd === io.rs1_sel)) &&
          // IF NOT EX/MEM HAZARD
          ~((io.EX_rd =/= "b00000".U) && (io.EX_rd === io.rs1_sel)) &&
          (io.MEM_rd === io.rs1_sel)) {

          io.forward_a := "b0011".U

        } .elsewhen(io.MEM_rd =/= "b00000".U && io.MEM_MemRead === 1.U &&
        // IF NOT ALU HAZARD
        ~((io.ID_rd =/= "b00000".U) && (io.ID_rd === io.rs1_sel) && (io.ID_rd === io.rs2_sel)) &&
        // IF NOT EX/MEM HAZARD
        ~((io.EX_rd =/= "b00000".U) && (io.EX_rd === io.rs1_sel) && (io.EX_rd === io.rs2_sel)) &&
        (io.MEM_rd === io.rs1_sel) && (io.MEM_rd === io.rs2_sel)) {
        // FOR Load instructions
        io.forward_a := "b0101".U
        io.forward_b := "b0101".U

      }
        .elsewhen(io.MEM_rd =/= "b00000".U && io.MEM_MemRead === 1.U &&
          // IF NOT ALU HAZARD
          ~((io.ID_rd =/= "b00000".U) && (io.ID_rd === io.rs2_sel)) &&
          // IF NOT EX/MEM HAZARD
          ~((io.EX_rd =/= "b00000".U) && (io.EX_rd === io.rs2_sel)) &&
          (io.MEM_rd === io.rs2_sel)) {

          io.forward_b := "b0101".U

        }
        .elsewhen(io.MEM_rd =/= "b00000".U && io.MEM_MemRead === 1.U &&
          // IF NOT ALU HAZARD
          ~((io.ID_rd =/= "b00000".U) && (io.ID_rd === io.rs1_sel)) &&
          // IF NOT EX/MEM HAZARD
          ~((io.EX_rd =/= "b00000".U) && (io.EX_rd === io.rs1_sel))&&
          (io.MEM_rd === io.rs1_sel)) {

          io.forward_a := "b0101".U

        }

    }
      // Forwarding for JALR unit
      .elsewhen(io.control_branch === 0.U) {
        // ALU Hazard
        when(io.ID_rd =/= "b00000".U && io.ID_MemRead =/= 1.U && (io.ID_rd === io.rs1_sel)){
          io.forward_a := "b0110".U
        }

        // EX/MEM Hazard
        when(io.EX_rd =/= "b00000".U && io.EX_MemRead =/= 1.U &&
          ~((io.ID_rd =/= "b00000".U) && (io.ID_rd === io.rs1_sel)) &&
          (io.EX_rd === io.rs1_sel)) {

          io.forward_a := "b0111".U

        }
          .elsewhen(io.EX_rd =/= "b00000".U && io.EX_MemRead === 1.U &&
            ~((io.ID_rd =/= "b00000".U) && (io.ID_rd === io.rs1_sel)) &&
            (io.EX_rd === io.rs1_sel)) {
            // FOR Load instructions
            io.forward_a := "b1001".U

        }


        // MEM/WB Hazard
        when(io.MEM_rd =/= "b00000".U && io.MEM_MemRead =/= 1.U &&
          // IF NOT ALU HAZARD
          ~((io.ID_rd =/= "b00000".U) && (io.ID_rd === io.rs1_sel)) &&
          // IF NOT EX/MEM HAZARD
          ~((io.EX_rd =/= "b00000".U) && (io.EX_rd === io.rs1_sel)) &&
          (io.MEM_rd === io.rs1_sel)) {

          io.forward_a := "b1000".U

        }
           .elsewhen(io.MEM_rd =/= "b00000".U && io.MEM_MemRead === 1.U &&
          // IF NOT ALU HAZARD
          ~((io.ID_rd =/= "b00000".U) && (io.ID_rd === io.rs1_sel)) &&
          // IF NOT EX/MEM HAZARD
          ~((io.EX_rd =/= "b00000".U) && (io.EX_rd === io.rs1_sel)) &&
          (io.MEM_rd === io.rs1_sel)) {
          // FOR Load instructions
          io.forward_a := "b1010".U

        }


      }
}
