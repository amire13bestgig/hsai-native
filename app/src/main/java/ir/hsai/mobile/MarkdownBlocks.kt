package ir.hsai.mobile

sealed interface MarkdownBlock {
 data class Line(val text:String):MarkdownBlock
 data class Code(val text:String):MarkdownBlock
 data class Table(val header:List<String>,val rows:List<List<String>>):MarkdownBlock
}
object MarkdownBlocks {
 fun cells(line:String):List<String> {
  var value=line.trim();if(value.startsWith('|'))value=value.drop(1)
  if(value.endsWith('|')&&!value.endsWith("\\|"))value=value.dropLast(1)
  val out=mutableListOf<String>();val cell=StringBuilder();var escaped=false;var code=false
  value.forEach { c->when {
   escaped->{if(c!='|'&&c!='\\')cell.append('\\');cell.append(c);escaped=false}
   c=='\\'->escaped=true
   c=='`'->{code=!code;cell.append(c)}
   c=='|'&&!code->{out.add(cell.toString().trim());cell.clear()}
   else->cell.append(c)
  }}
  if(escaped)cell.append('\\');out.add(cell.toString().trim());return out
 }
 fun parse(text:String):List<MarkdownBlock> {
  val lines=text.lines();val out=mutableListOf<MarkdownBlock>();var i=0
  while(i<lines.size) {
   val line=lines[i]
   val fence=line.trimStart().takeWhile{it=='`'||it=='~'}
   if(fence.length>=3&&fence.toSet().size==1) {
    val code=mutableListOf<String>();i++
    while(i<lines.size&&!lines[i].trim().startsWith(fence)){code.add(lines[i++])}
    if(i<lines.size)i++
    out.add(MarkdownBlock.Code(code.joinToString("\n")));continue
   }
   if('|' in line&&i+1<lines.size&&cells(lines[i+1]).isNotEmpty()&&cells(lines[i+1]).all{Regex(":?-{3,}:?").matches(it)}) {
    val header=cells(line);i+=2;val rows=mutableListOf<List<String>>()
    while(i<lines.size&&'|' in lines[i]&&lines[i].isNotBlank())rows.add(cells(lines[i++]))
    out.add(MarkdownBlock.Table(header,rows));continue
   }
   out.add(MarkdownBlock.Line(line));i++
  }
  return out
 }
}
