package ir.hsai.mobile

import org.json.JSONArray
import org.json.JSONObject

/** Serialize explicit user inputs; fees, rates and legal conclusions are never calculated locally. */
object CalculatorContract {
 fun fields(calculator:Calculator,values:Map<String,String>)=calculator.fields.filterNot{it.key=="agreed_fee_toman"&&values["agreement_mode"]!="agreed"}
 fun body(calculator:Calculator,values:Map<String,String>):JSONObject {
  val body=JSONObject().put("type",calculator.id);val errors=linkedMapOf<String,String>()
  fields(calculator,values).forEach { field->
   val raw=values[field.key]?:field.options.firstOrNull()?.first.orEmpty()
   try {
    if(field.multiple){
     val selected=raw.split(',').map{it.trim()}.filter{it.isNotEmpty()}.distinct()
     require(selected.isNotEmpty()&&selected.all{key->field.options.any{it.first==key}}){"حداقل یک خدمت معتبر انتخاب کنید."}
     body.put(field.key,JSONArray(selected))
    }else {
     if(field.options.isNotEmpty())require(field.options.any{it.first==raw}){"یکی از گزینه‌های معتبر را انتخاب کنید."}
     val value=when {field.boolean->raw=="true";field.numeric->Validation.number(field.key,raw);field.key=="heir_class"->raw.toInt();else->raw}
     if(field.key=="copies")require((value as Number).toDouble()>=1&&value.toDouble()%1.0==0.0){"تعداد نسخه باید عدد صحیح و حداقل یک باشد."}
     body.put(field.key,value)
    }
   }catch(e:IllegalArgumentException){errors[field.key]=e.message?:"ورودی معتبر نیست."}
  }
  if(errors.isEmpty()&&calculator.id=="delay_damage"&&body.getInt("pay_year")*12+body.getInt("pay_month")<body.getInt("due_year")*12+body.getInt("due_month"))errors["pay_year"]="تاریخ پرداخت پیش از سررسید است."
  if(errors.isNotEmpty())throw FormValidationException(errors)
  return body
 }
}
