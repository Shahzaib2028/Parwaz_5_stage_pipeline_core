package riscv
import chisel3._
import chisel3.iotesters.PeekPokeTester
class MainTests(c: Main) extends PeekPokeTester(c) {
  //poke(c.io.instruction,"b00000000000100000000000110110011".U)
  //poke(c.io.pc,0)
  //poke(c.io.pc,0)
  step(60)


  //expect(c.io.AluOut,9)
  //expect(c.io.branchCheck,0)
}
