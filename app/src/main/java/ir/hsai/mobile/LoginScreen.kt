package ir.hsai.mobile

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import org.json.JSONObject

/** Authentication is an explicit route, shown after a protected action; guest chat stays separate. */
@Composable fun LoginScreen(s:AppUiState,vm:AppViewModel) {
 var mode by rememberSaveable{mutableStateOf("login")}
 var identifier by rememberSaveable{mutableStateOf("")}
 var name by rememberSaveable{mutableStateOf("")}
 var phone by rememberSaveable{mutableStateOf("")}
 var email by rememberSaveable{mutableStateOf("")}
 // Password deliberately never enters saved instance state, preferences or the source tree.
 var password by remember{mutableStateOf("")}
 val focus=LocalFocusManager.current
 val submit:()->Unit={focus.clearFocus();if(mode=="otp")vm.sendOtp(phone)else vm.authenticate(mode=="register",mapOf("identifier" to identifier,"full_name" to name,"phone" to phone,"email" to email,"password" to password))}
 Box(Modifier.fillMaxSize().background(SiteNavy)) {
  LegalGrid(Modifier.fillMaxSize())
  BoxWithConstraints(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding(),contentAlignment=Alignment.Center) {
   val wide=maxWidth>=1024.dp&&androidx.compose.ui.platform.LocalDensity.current.fontScale<1.4f
   val form:@Composable ()->Unit = {
   Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(20.dp)) {
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
     Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
      BrandLockup("lg")
     }
     TextButton(onClick=vm::browseAsGuest,enabled=!s.posting,shape=RoundedCornerShape(12.dp),colors=ButtonDefaults.textButtonColors(containerColor=Color.White.copy(alpha=.1f)),contentPadding=PaddingValues(8.dp)) {Text("بازگشت به چت",color=Palette.Slate300,style=MaterialTheme.typography.labelSmall)}
    }
    Surface(Modifier.fillMaxWidth(),shape=SiteShape.Card,color=Soft.CardLight,border=BorderStroke(1.dp,Soft.BorderLight),shadowElevation=4.dp) {
     Column(Modifier.padding(24.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
      Text(if(mode=="register")"ایجاد حساب کاربری حق‌سنج"else "ورود به میز کار حق‌سنج",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Black)
      if(!s.pendingGuestMessage)Text(if(s.config.optBoolean("free_trial_enabled",false))"با ورود یا عضویت، هدیه مشاوره رایگان شما بلافاصله فعال می‌شود."else "برای دسترسی به میز کار حقوقی و اسناد قضایی خود وارد شوید.",color=MaterialTheme.colorScheme.onSurfaceVariant,style=MaterialTheme.typography.bodySmall)
      if(s.pendingGuestMessage)Surface(color=MaterialTheme.colorScheme.secondaryContainer,shape=RoundedCornerShape(14.dp)) {
       Text("پیامت محفوظ است؛ برای دریافت پاسخ وارد شو.",Modifier.fillMaxWidth().padding(12.dp),color=MaterialTheme.colorScheme.onSecondaryContainer,style=MaterialTheme.typography.bodyMedium,fontWeight=FontWeight.Bold)
      }
      if(s.authenticated) {
       Text("خوش آمدید، ${s.user.str("full_name","دوست عزیز")}",fontWeight=FontWeight.Medium)
       Text("نشست امن شما روی این دستگاه فعال است.",style=MaterialTheme.typography.bodySmall)
       Button(onClick=vm::enterWorkspace,enabled=s.initialized&&!s.posting,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp),shape=SiteShape.Xl){Text("ادامه به میز کار")}
       SoftButton(onClick={vm.logout()},enabled=!s.posting,modifier=Modifier.fillMaxWidth()){Text("ورود با حساب دیگر")}
      } else {
       if(s.config.optBoolean("sms_otp_enabled")) {
        Surface(color=Soft.well(),shape=SiteShape.Xxl,border=BorderStroke(1.dp,Soft.border())) {
         Row(Modifier.padding(4.dp)) {
          listOf("otp" to "ورود پیامکی","login" to "ایمیل و رمز عبور").forEach { (key,label)->
           val selected=if(key=="otp")mode=="otp"else mode!="otp"
           TextButton(onClick={mode=key;vm.clearFormErrors()},enabled=!s.posting,modifier=Modifier.weight(1f),shape=RoundedCornerShape(12.dp),colors=ButtonDefaults.textButtonColors(containerColor=if(selected)SiteNavy else Color.Transparent,contentColor=if(selected)Palette.Brass300 else Palette.Slate500)) {Text(label,style=MaterialTheme.typography.labelLarge,fontWeight=FontWeight.Bold)}
          }
         }
        }
       }
       if(mode!="otp")Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        AuthTab("ورود با رمز عبور",mode=="login",Modifier.weight(1f),enabled=!s.posting){mode="login";vm.clearFormErrors()}
        AuthTab("عضویت جدید",mode=="register",Modifier.weight(1f),enabled=!s.posting){mode="register";vm.clearFormErrors()}
       }
       if(mode=="register")AuthField("نام و نام خانوادگی رسمی",name,{name=it},enabled=!s.posting,error=s.fieldErrors["full_name"],onEdited={vm.inputEdited("full_name")},firstError=s.firstField("full_name"),attempt=s.validationAttempt)
       if(mode=="login")AuthField("آدرس ایمیل یا شماره موبایل",identifier,{identifier=it},placeholder="پست الکترونیک یا شماره موبایل",latin=true,enabled=!s.posting,error=s.fieldErrors["identifier"],onEdited={vm.inputEdited("identifier")},firstError=s.firstField("identifier"),attempt=s.validationAttempt)
       else AuthField("شماره موبایل",phone,{phone=it},placeholder="۰۹۱۲۳۴۵۶۷۸۹",numeric=true,latin=true,enabled=!s.posting,error=s.fieldErrors["phone"],onEdited={vm.inputEdited("phone")},firstError=s.firstField("phone"),attempt=s.validationAttempt)
       if(mode=="register")AuthField("نشانی ایمیل (اختیاری)",email,{email=it},latin=true,enabled=!s.posting,error=s.fieldErrors["email"],onEdited={vm.inputEdited("email")},firstError=s.firstField("email"),attempt=s.validationAttempt)
       if(mode!="otp") {
        Column(verticalArrangement=Arrangement.spacedBy(2.dp)) {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
         Text("رمز عبور",style=MaterialTheme.typography.labelLarge,fontWeight=FontWeight.Bold)
         if(mode=="login")TextButton(onClick={vm.openDialog("forgot","بازیابی رمز عبور")},contentPadding=PaddingValues(0.dp)){Text("فراموشی رمز عبور؟",color=Copper,style=MaterialTheme.typography.labelMedium)}
        }
        AuthField("رمز عبور",password,{password=it},password=true,latin=true,showLabel=false,enabled=!s.posting,onDone=submit,error=s.fieldErrors["password"],onEdited={vm.inputEdited("password")},firstError=s.firstField("password"),attempt=s.validationAttempt)
        }
        if(mode=="register")PasswordStrength(password)
       }
       ErrorNotice(s)
       Button(onClick=submit,enabled=s.initialized&&!s.posting,modifier=Modifier.fillMaxWidth().heightIn(min=50.dp),shape=SiteShape.Xl,colors=ButtonDefaults.buttonColors(containerColor=Palette.Navy950,contentColor=Palette.Brass300,disabledContainerColor=Soft.WellLight,disabledContentColor=Palette.Slate500)) {
        if(s.posting||!s.initialized){CircularProgressIndicator(Modifier.size(18.dp),strokeWidth=2.dp,color=MaterialTheme.colorScheme.onPrimary);Spacer(Modifier.width(8.dp))}
        Text(if(mode=="otp")"دریافت کد ورود"else if(mode=="register")"عضویت و شروع مشاوره"else "ورود به حساب کاربری",fontWeight=FontWeight.Bold)
       }
       /**
        * ورود با گوگل — قبلاً این دکمه عمداً نبود چون قرارداد بومی وجود نداشت.
        * اکنون قرارداد بومی هست: nonce دستگاه + برگهٔ دست‌دهی کوتاه‌عمر
        * (POST /api/v1/auth/google/handoff). جریان در مرورگر امن انجام می‌شود.
        */
       if(s.config.optBoolean("google_sign_in_enabled",false)) {
        SoftButton(onClick={vm.startGoogleSignIn()},enabled=!s.posting,modifier=Modifier.fillMaxWidth()){Text("ورود با حساب گوگل")}
        Text("ورود با گوگل در مرورگر امن گوشی انجام می‌شود و پس از تأیید، خودکار به اپ برمی‌گردید.",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
       }
       // فرم تماس عمومی سایت: برای کسی که هنوز حساب ندارد هم باز است.
       TextButton(onClick={vm.openDialog("contact","تماس با پشتیبانی")},enabled=!s.posting){Text("پیام به پشتیبانی (بدون حساب)",color=Copper,style=MaterialTheme.typography.labelMedium)}
      }
      Text(buildAnnotatedString {
       append("ورود شما به معنای پذیرش ")
       withLink(LinkAnnotation.Url("https://hsai.ir/terms")){append("قوانین استفاده")}
       append(" و ")
       withLink(LinkAnnotation.Url("https://hsai.ir/privacy")){append("سیاست حریم خصوصی حق‌سنج")}
       append(" است.")
      },style=MaterialTheme.typography.bodySmall,textAlign=TextAlign.Center,color=MaterialTheme.colorScheme.onSurfaceVariant)

     }
    }
   }
   }
   if(wide)Row(Modifier.widthIn(max=1056.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp),horizontalArrangement=Arrangement.spacedBy(32.dp),verticalAlignment=Alignment.CenterVertically) {
    Box(Modifier.weight(1f),contentAlignment=Alignment.Center){Column(Modifier.widthIn(max=448.dp).fillMaxWidth()){form()}}
    Box(Modifier.weight(1f).padding(start=16.dp)){AuthShowcase()}
   } else Column(Modifier.widthIn(max=480.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal=16.dp,vertical=32.dp)){form()}
  }
 }
}

@Composable private fun AuthTab(text:String,selected:Boolean,modifier:Modifier,enabled:Boolean=true,onClick:()->Unit) {
 Column(modifier) {
  TextButton(onClick=onClick,enabled=enabled,modifier=Modifier.fillMaxWidth(),contentPadding=PaddingValues(vertical=8.dp,horizontal=0.dp)){Text(text,style=MaterialTheme.typography.labelLarge,fontWeight=if(selected)FontWeight.Bold else FontWeight.Normal,color=if(selected)SiteNavy else Color(0xFF475569))}
  HorizontalDivider(color=if(selected)Palette.Brass400 else Soft.HairlineLight,thickness=if(selected)2.dp else 1.dp)
 }
}
@Composable private fun AuthField(label:String,value:String,onValue:(String)->Unit,placeholder:String="",password:Boolean=false,numeric:Boolean=false,latin:Boolean=false,showLabel:Boolean=true,enabled:Boolean=true,onDone:(()->Unit)?=null,error:String?=null,firstError:Boolean=false,attempt:Int=0,onEdited:(()->Unit)?=null) {
 var visible by remember{mutableStateOf(false)}
 Column(verticalArrangement=Arrangement.spacedBy(6.dp)) {
  if(showLabel)Text(label,style=MaterialTheme.typography.labelLarge,fontWeight=FontWeight.Bold)
  CompositionLocalProvider(LocalLayoutDirection provides if(latin)LayoutDirection.Ltr else LayoutDirection.Rtl) {
   OutlinedTextField(value=value,onValueChange={onValue(it);onEdited?.invoke()},enabled=enabled,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp).then(Modifier.semanticsLabel(label)).then(Modifier.fieldFeedback(error,firstError,attempt)),isError=!error.isNullOrBlank(),supportingText=if(!error.isNullOrBlank()){{Text(error)}}else null,singleLine=true,shape=SiteShape.Xl,
    placeholder={Text(if(password)"••••••••"else placeholder,style=MaterialTheme.typography.bodyMedium)},
    visualTransformation=if(password&&!visible)PasswordVisualTransformation()else VisualTransformation.None,
    keyboardOptions=KeyboardOptions(keyboardType=if(password)KeyboardType.Password else if(numeric)KeyboardType.Phone else if(latin)KeyboardType.Email else KeyboardType.Text,imeAction=if(onDone!=null)ImeAction.Done else ImeAction.Next),
    keyboardActions=KeyboardActions(onDone={onDone?.invoke()},onNext={defaultKeyboardAction(ImeAction.Next)}),
    leadingIcon=if(password){{IconButton(onClick={visible=!visible}){Icon(if(visible)NativeIcons.VisibilityOff else NativeIcons.Visibility,if(visible)"پنهان کردن رمز"else "نمایش رمز")}}}else null,
    trailingIcon={Icon(if(password)NativeIcons.Lock else if(numeric)NativeIcons.Phone else NativeIcons.Mail,null,Modifier.size(20.dp))},
    colors=OutlinedTextFieldDefaults.colors(unfocusedContainerColor=Soft.WellLight,focusedContainerColor=Soft.WellLight,unfocusedBorderColor=Color.Transparent,focusedBorderColor=Palette.Brass400,errorBorderColor=MaterialTheme.colorScheme.error,cursorColor=Palette.Brass500),textStyle=MaterialTheme.typography.bodyMedium)
  }
 }
}
private fun Modifier.semanticsLabel(label:String):Modifier = this.then(Modifier.semantics { contentDescription=label })

internal fun passwordStrengthScore(value:String):Int {
 var score=0
 if(value.length>=8)score++
 if(value.length>=11)score++
 if(value.any(Char::isUpperCase)&&value.any(Char::isLowerCase))score++
 if(value.any{it.isDigit()||!it.isLetterOrDigit()})score++
 return score
}
@Composable private fun PasswordStrength(value:String) {
 if(value.isEmpty())return
 val score=passwordStrengthScore(value)
 val labels=listOf("خیلی کوتاه","ضعیف","متوسط","قوی","بسیار امن")
 val colors=listOf(Color(0xFFF43F5E),Color(0xFFF59E0B),Color(0xFFEAB308),Color(0xFF10B981),Color(0xFF059669))
 Column(verticalArrangement=Arrangement.spacedBy(6.dp)) {
  Row(horizontalArrangement=Arrangement.spacedBy(4.dp)){repeat(4){i->Box(Modifier.weight(1f).height(4.dp).background(if(i<score)colors[score]else Soft.HairlineLight,RoundedCornerShape(4.dp)))}}
  Text("قدرت رمز: ${labels[score]}",style=MaterialTheme.typography.labelSmall,color=colors[score])
 }
}
