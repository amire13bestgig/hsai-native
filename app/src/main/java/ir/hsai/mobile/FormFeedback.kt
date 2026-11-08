@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
package ir.hsai.mobile

import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.error as fieldError

class FormValidationException(val fields:Map<String,String>):IllegalArgumentException(fields.values.firstOrNull()?:"ورودی‌ها را بررسی کنید.")
fun validateFields(vararg rules:Pair<String,()->Unit>) {
 val errors=linkedMapOf<String,String>()
 rules.forEach{(key,rule)->try{rule()}catch(e:IllegalArgumentException){errors[key]=e.message?:"ورودی معتبر نیست."}catch(_:java.time.format.DateTimeParseException){errors[key]="تاریخ معتبر انتخاب کنید."}}
 if(errors.isNotEmpty())throw FormValidationException(errors)
}
fun AppUiState.firstField(key:String)=fieldErrors.keys.firstOrNull()==key
@Composable fun Modifier.fieldFeedback(error:String?,first:Boolean,attempt:Int):Modifier {
 val focus=remember{FocusRequester()};val bring=remember{BringIntoViewRequester()}
 LaunchedEffect(attempt){if(!error.isNullOrBlank()&&first){focus.requestFocus();bring.bringIntoView()}}
 return this.focusProperties{canFocus=true}.focusRequester(focus).bringIntoViewRequester(bring).semantics{if(!error.isNullOrBlank())fieldError(error)}
}
