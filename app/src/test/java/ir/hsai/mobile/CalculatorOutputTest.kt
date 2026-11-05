package ir.hsai.mobile

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

/** Arithmetic checks against public observed outputs, not a certification of legal rates. */
class CalculatorOutputTest {
 private val all=JSONObject(javaClass.getResource("/calculator-observed-output.json")!!.readText())
 @Test fun independentArithmeticForNineCalculators() {
  assertEquals(20_000_000L*25/1000+130_000_000L*35/1000,all.getJSONObject("court_fee").getLong("first_stage_fee_toman"))
  assertEquals(110L*100_000_000,all.getJSONObject("mehrieh").getLong("total_value_toman"))
  val labor=all.getJSONObject("labor");assertEquals(12_000_000L+24_000_000L,labor.getLong("total_receivable_toman"))
  val fee=all.getJSONObject("lawyer_fee");assertEquals(50_000_000L*8/100+100_000_000L*7/100,fee.getLong("total_official_fee_toman"));assertEquals(fee.getLong("total_official_fee_toman"),fee.getLong("first_stage_share_toman")+fee.getLong("appeal_stage_share_toman"))
  assertEquals(1_600_000_000L/10,all.getJSONObject("diyeh").getLong("payable_diyeh_toman"))
  assertEquals(500_000_000L*3/100+600_000_000L*2/100+800_000_000L*75/1000,all.getJSONObject("inheritance_tax").getLong("total_inheritance_tax_toman"))
  assertEquals(100_000_000L*5/100,all.getJSONObject("execution_fee").getLong("payable_execution_fee_toman"))
  val capital=all.getJSONObject("capital_gains_tax");val gain=2_000_000_000.0-1_000_000_000.0*1.3*1.3
  assertEquals(gain,capital.getDouble("inflation_adjusted_gain_toman"),1.0);assertEquals(gain*.25,capital.getDouble("payable_tax_toman"),1.0)
  val shares=all.getJSONObject("inheritance").getJSONObject("shares")
  assertEquals(125_000_000L,shares.getLong("spouse_toman"));assertEquals(1_000_000_000L,shares.getLong("spouse_toman")+shares.getLong("each_son_toman")+shares.getLong("each_daughter_toman"))
  assertEquals(2.0,shares.getDouble("each_son_toman")/shares.getDouble("each_daughter_toman"),0.00001)
 }
 @Test fun delayDamageUsesUnroundedCpiRatio() {
  val d=all.getJSONObject("delay_damage");val expected=d.getDouble("principal_toman")*(d.getDouble("cpi_pay")/d.getDouble("cpi_due")-1)
  assertEquals(expected,d.getDouble("delay_damage_toman"),1.0)
  assertEquals(d.getLong("principal_toman")+d.getLong("delay_damage_toman"),d.getLong("total_payable_toman"))
 }
}
