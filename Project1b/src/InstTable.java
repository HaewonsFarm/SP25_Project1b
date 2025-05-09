import java.io.*;
import java.util.HashMap;


/**
 * 모든 instruction의 정보를 관리하는 클래스. instruction data들을 저장한다. <br>
 * 또한 instruction 관련 연산, 예를 들면 목록을 구축하는 함수, 관련 정보를 제공하는 함수 등을 제공 한다.
 */
public class InstTable {
	/**
	 * inst.data 파일을 불러와 저장하는 공간.
	 *  명령어의 이름을 집어넣으면 해당하는 Instruction의 정보들을 리턴할 수 있다.
	 */
	public HashMap<String, Instruction> instMap;

	/**
	 * 클래스 초기화. 파싱을 동시에 처리한다.
	 * @param instFile : instuction에 대한 명세가 저장된 파일 이름
	 */
	public InstTable(String instFile) {
		instMap = new HashMap<>();
		openFile(instFile);
	}

	// 주어진 mnemonic에 따라 SIC/XE 명령어 형식 길이를 반환.
	// '+'로 시작하면 format 4, 아니면 instMap에 정의된 format 필드 반환.
	public int getInstructionLength(String op) {
		if (op == null || op.isEmpty()) return 0;
		boolean extended = op.startsWith("+");
		String key = extended ? op.substring(1) : op.toUpperCase();
		Instruction inst = instMap.get(key);
		if (inst == null) return 0;
		return extended ? 4 : inst.format;
	}

	public Instruction getInst(String mnemonic) {
		return instMap.get(mnemonic.toUpperCase());
	}

	/**
	 * 입력받은 이름의 파일을 열고 해당 내용을 파싱하여 instMap에 저장한다.
	 */
	public void openFile(String fileName) {
		try (BufferedReader br = new BufferedReader(
				new InputStreamReader(
						getClass().getClassLoader().getResourceAsStream(fileName)
				))) {
			String line;
			while ((line = br.readLine()) != null) {
				if (!line.trim().isEmpty()) {
					Instruction inst = new Instruction(line);
					instMap.put(inst.mnemonic.toUpperCase(), inst);
				}
			}
		} catch (Exception e) {
			System.err.println("⚠ Failed to load resource: " + fileName);
			e.printStackTrace();
		}
	}

	//get, set, search 등의 함수는 자유 구현

}
/**
 * 명령어 하나하나의 구체적인 정보는 Instruction클래스에 담긴다.
 * instruction과 관련된 정보들을 저장하고 기초적인 연산을 수행한다.
 */
class Instruction {
	String mnemonic;
	int format;
	int opcode;
	int operandCount;

	public Instruction(String line) {
		parsing(line);
	}

	public void parsing(String line) {
		String[] parts = line.trim().split("\\s+");
		if (parts.length < 3) {
			throw new IllegalArgumentException("Invalid instruction format: " + line);
		}
		mnemonic = parts[0];
		format = Integer.parseInt(parts[1]);
		opcode = Integer.parseInt(parts[2], 16); // 16진수 파싱
		operandCount = (parts.length >= 4) ? Integer.parseInt(parts[3]) : 0;
	}
}