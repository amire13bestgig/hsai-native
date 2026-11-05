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
import androidx.compose.ui.geometry.Offset
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

/** Authentication is a separate, first route; it never displays workspace navigation behind it. */
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
  Canvas(Modifier.fillMaxSize()) {
   val step=38.dp.toPx();val line=Color.White.copy(alpha=.045f)
   var x=0f;while(x<size.width){drawLine(line,Offset(x,0f),Offset(x,size.height));x+=step}
   var y=0f;while(y<size.height){drawLine(line,Offset(0f,y),Offset(size.width,y));y+=step}
  }
  Box(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding(),contentAlignment=Alignment.Center) {
   Column(Modifier.widthIn(max=480.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal=16.dp,vertical=32.dp),verticalArrangement=Arrangement.spacedBy(28.dp)) {
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
     Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
      Surface(shape=RoundedCornerShape(12.dp),color=Color.Transparent,border=BorderStroke(1.dp,SiteGold)) {Icon(NativeIcons.Balance,null,Modifier.padding(8.dp).size(24.dp),tint=SiteGold)}
      Column{Text("حق‌سنج",color=Color.White,fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleMedium);Text("اول حقت را بسنج، بعد قدم بردار",color=SiteGold,style=MaterialTheme.typography.labelSmall)}
     }
     TextButton(onClick={vm.openExternal("https://hsai.ir")},contentPadding=PaddingValues(8.dp)) {Text("بازگشت به سایت",color=Color(0xFFDDE4ED),style=MaterialTheme.typography.labelSmall)}
    }
    Surface(Modifier.fillMaxWidth(),shape=RoundedCornerShape(24.dp),color=Color.White,border=BorderStroke(1.dp,Color(0xFFDDE4ED))) {
     Column(Modifier.padding(24.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
      Text("ورود به میز کار حق‌سنج",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
      Text("با ورود یا عضویت، هدیه مشاوره رایگان شما بلافاصله فعال می‌شود.",color=MaterialTheme.colorScheme.onSurfaceVariant,style=MaterialTheme.typography.bodySmall)
      if(s.authenticated) {
       Text("خوش آمدید، ${s.user.str("full_name","دوست عزیز")}",fontWeight=FontWeight.Medium)
       Text("نشست امن شما روی این دستگاه فعال است.",style=MaterialTheme.typography.bodySmall)
       Button(onClick=vm::enterWorkspace,enabled=s.initialized&&!s.posting,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp),shape=RoundedCornerShape(16.dp)){Text("ادامه به میز کار")}
       OutlinedButton(onClick={vm.logout()},enabled=!s.posting,modifier=Modifier.fillMaxWidth()){Text("ورود با حساب دیگر")}
      } else {
       Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        AuthTab("ورود با رمز عبور",mode=="login",Modifier.weight(1f)){mode="login"}
        AuthTab("عضویت جدید",mode=="register",Modifier.weight(1f)){mode="register"}
       }
       if(s.config.optBoolean("sms_otp_enabled"))TextButton(onClick={mode=if(mode=="otp")"login"else "otp"},enabled=!s.posting){Text(if(mode=="otp")"بازگشت به ورود با رمز"else "ورود سریع با کد پیامکی")}
       if(mode=="register")AuthField("نام و نام خانوادگی رسمی",name,{name=it},enabled=!s.posting)
       if(mode=="login")AuthField("آدرس ایمیل یا شماره موبایل",identifier,{identifier=it},placeholder="پست الکترونیک یا شماره موبایل",latin=true,enabled=!s.posting)
       else AuthField("شماره موبایل",phone,{phone=it},placeholder="۰۹۱۲۳۴۵۶۷۸۹",numeric=true,latin=true,enabled=!s.posting)
       if(mode=="register")AuthField("نشانی ایمیل (اختیاری)",email,{email=it},latin=true,enabled=!s.posting)
       if(mode!="otp") {
        Column(verticalArrangement=Arrangement.spacedBy(2.dp)) {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
         Text("رمز عبور",style=MaterialTheme.typography.labelLarge,fontWeight=FontWeight.Bold)
         if(mode=="login")TextButton(onClick={vm.openDialog("external","بازیابی رمز عبور",JSONObject().put("url","https://hsai.ir/auth/forgot-password"))},contentPadding=PaddingValues(0.dp)){Text("فراموشی رمز عبور؟",color=Copper,style=MaterialTheme.typography.labelMedium)}
        }
        AuthField("رمز عبور",password,{password=it},password=true,latin=true,showLabel=false,enabled=!s.posting,onDone=submit)
        }
       }
       ErrorNotice(s)
       Button(onClick=submit,enabled=s.initialized&&!s.posting,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp),shape=RoundedCornerShape(16.dp)) {
        if(s.posting||!s.initialized){CircularProgressIndicator(Modifier.size(18.dp),strokeWidth=2.dp,color=MaterialTheme.colorScheme.onPrimary);Spacer(Modifier.width(8.dp))}
        Text(if(mode=="otp")"دریافت کد ورود"else if(mode=="register")"عضویت و شروع مشاوره"else "ورود به حساب کاربری",fontWeight=FontWeight.Bold)
       }
       // The official native API has no Google OAuth callback contract. Do not simulate a successful native sign-in.
       HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
       Text("ورود گوگل در مرورگر انجام می‌شود و به اپ منتقل نمی‌شود.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant,textAlign=TextAlign.Center)
       OutlinedButton(onClick={vm.openExternal("https://hsai.ir/auth")},modifier=Modifier.fillMaxWidth().heightIn(min=48.dp),shape=RoundedCornerShape(16.dp)){Text("ورود با حساب گوگل در وب‌سایت")}
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
 }
}

@Composable private fun AuthTab(text:String,selected:Boolean,modifier:Modifier,onClick:()->Unit) {
 Column(modifier) {
  TextButton(onClick=onClick,modifier=Modifier.fillMaxWidth(),contentPadding=PaddingValues(vertical=8.dp,horizontal=0.dp)){Text(text,style=MaterialTheme.typography.labelLarge,fontWeight=if(selected)FontWeight.Bold else FontWeight.Normal,color=if(selected)SiteNavy else Color(0xFF475569))}
  HorizontalDivider(color=if(selected)SiteGold else Color(0xFFF1F5F9),thickness=if(selected)2.dp else 1.dp)
 }
}
@Composable private fun AuthField(label:String,value:String,onValue:(String)->Unit,placeholder:String="",password:Boolean=false,numeric:Boolean=false,latin:Boolean=false,showLabel:Boolean=true,enabled:Boolean=true,onDone:(()->Unit)?=null) {
 var visible by remember{mutableStateOf(false)}
 Column(verticalArrangement=Arrangement.spacedBy(6.dp)) {
  if(showLabel)Text(label,style=MaterialTheme.typography.labelLarge,fontWeight=FontWeight.Bold)
  CompositionLocalProvider(LocalLayoutDirection provides if(latin)LayoutDirection.Ltr else LayoutDirection.Rtl) {
   OutlinedTextField(value=value,onValueChange=onValue,enabled=enabled,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp).then(Modifier.semanticsLabel(label)),singleLine=true,shape=RoundedCornerShape(16.dp),
    placeholder={Text(if(password)"••••••••"else placeholder,style=MaterialTheme.typography.bodyMedium)},
    visualTransformation=if(password&&!visible)PasswordVisualTransformation()else VisualTransformation.None,
    keyboardOptions=KeyboardOptions(keyboardType=if(password)KeyboardType.Password else if(numeric)KeyboardType.Phone else if(latin)KeyboardType.Email else KeyboardType.Text,imeAction=if(onDone!=null)ImeAction.Done else ImeAction.Next),
    keyboardActions=KeyboardActions(onDone={onDone?.invoke()},onNext={defaultKeyboardAction(ImeAction.Next)}),
    leadingIcon=if(password){{IconButton(onClick={visible=!visible}){Icon(if(visible)NativeIcons.VisibilityOff else NativeIcons.Visibility,if(visible)"پنهان کردن رمز"else "نمایش رمز")}}}else null,
    trailingIcon={Icon(if(password)NativeIcons.Lock else if(numeric)NativeIcons.Phone else NativeIcons.Mail,null,Modifier.size(20.dp))},
    colors=OutlinedTextFieldDefaults.colors(unfocusedContainerColor=Color(0xFFF8FAFC),focusedContainerColor=Color(0xFFF8FAFC),unfocusedBorderColor=Color(0xFFDDE4ED),focusedBorderColor=SiteGold),textStyle=MaterialTheme.typography.bodyMedium)
  }
 }
}
private fun Modifier.semanticsLabel(label:String):Modifier = this.then(Modifier.semantics { contentDescription=label })
