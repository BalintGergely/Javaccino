package net.balintgergely.puzzles.bp;

import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Pattern;

/* 
 * A mystery...
 */
public class Ruminate{
	private static final List<Pattern> PATTERNS = List.of(
		Pattern.compile("[fptscdbw][ahierlco][iourntae][cmnraeid][kersnatd]", Pattern.CASE_INSENSITIVE),
		Pattern.compile("[cptlsmwb][aeiouhrt][soivlrnt][ikvadths][aeonlrcs][ndgrwtey]", Pattern.CASE_INSENSITIVE),
		Pattern.compile("[vpfrlhcs][aeiouhlr][rpgaocmt][iustrdle][trlhaeiu][hltnrsez][ysghdern]", Pattern.CASE_INSENSITIVE),
		Pattern.compile("[dghlmnrt][aeiouhry][somraufn][pgtaeiou][cglnorst][taeiohnl][edstoluc][nrktyhes]", Pattern.CASE_INSENSITIVE),
		Pattern.compile("[brlptewums][crmnahweio][miysaldekb][8kehdpmnsz]", Pattern.CASE_INSENSITIVE)
	);
	public static void main(String[] atgs) throws Throwable{
		byte[] data;
		try(FileInputStream s = new FileInputStream("input.txt")){
			data = s.readAllBytes();
		}
		String textData = new String(data, StandardCharsets.UTF_8);
		for(Pattern pat : PATTERNS){
			System.out.println("Looking for " + pat.pattern());
			List<String> results = pat.matcher(textData).results().map(r -> r.group().toLowerCase()).sorted().distinct().toList();
			System.out.println(String.join(" ", results));
		}
	}
}
