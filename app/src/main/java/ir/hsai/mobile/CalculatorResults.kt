package ir.hsai.mobile

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.json.JSONObject

object CalculatorResults {
 val totals=mapOf("court_fee" to "total_initial_cost_toman","mehrieh" to "total_value_toman","delay_damage" to "total_payable_toman","inheritance" to "net_distributable_estate_toman","labor" to "total_receivable_toman","lawyer_fee" to "total_official_fee_toman","diyeh" to "payable_diyeh_toman","inheritance_tax" to "total_inheritance_tax_toman","execution_fee" to "payable_execution_fee_toman","capital_gains_tax" to "payable_tax_toman","expert_fee" to "payable_expert_fee_toman","arbitration_fee" to "payable_arbitration_fee_toman","notary_tariff" to "total_payable_toman","office_services_fee" to "total_payable_toman")
 val notes=setOf("legal_basis","rule_applied","start_basis_warning","haram_month_rule_note","wife_land_note","tax_note","exemption_note","debtor_responsibility_note","note")
 fun mainKey(type:String,data:JSONObject)=totals[type]?.takeIf{data.opt(it) is Number&&data.str("type",type)==type}
 fun remainder(data:JSONObject,consumed:Set<String>)=JSONObject().also{out->data.keys().forEach{if(it !in consumed)out.put(it,data.opt(it))}}
}
@Composable fun CalculatorResult(type:String,data:JSONObject) {
 val key=CalculatorResults.mainKey(type,data)
 if(key==null){if(data.has("type")&&data.str("type")!=type)Text("نوع نتیجه برگشتی با ابزار انتخاب‌شده هماهنگ نیست.",color=MaterialTheme.colorScheme.error);DataResult(data);return}
 var expanded by remember(type,data){mutableStateOf(false)}
 Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
  Text(Domain.labels[key]?:"مبلغ نتیجه",style=MaterialTheme.typography.labelLarge)
  Text(fa(data.opt(key) as Number)+" تومان",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
  val notes=CalculatorResults.notes.filter{data.has(it)&&!data.isNull(it)&&data.opt(it)?.toString().orEmpty().isNotBlank()}.toSet()
  if(notes.isNotEmpty())DataResult(JSONObject().also{out->notes.forEach{out.put(it,data.opt(it))}})
  val rest=CalculatorResults.remainder(data,notes+key)
  if(UiPresentation.keys(rest).isNotEmpty()) {
   TextButton(onClick={expanded=!expanded}){Text(if(expanded)"بستن ریز محاسبات"else "ریز محاسبات و ورودی‌ها")}
   if(expanded)DataResult(rest)
  }
 }
}
