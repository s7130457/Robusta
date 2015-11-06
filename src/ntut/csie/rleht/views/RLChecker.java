package ntut.csie.rleht.views;

import java.util.ArrayList;
import java.util.List;

import ntut.csie.rleht.common.ConsoleLog;

import org.apache.commons.lang.text.StrTokenizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RLChecker {
	private static Logger logger =LoggerFactory.getLogger(RLChecker.class);
	
	private final boolean DEBUG = false;

	private List<RLMessage> exList;

	private List<RLMessage> rlList;

	public List<RLMessage> check(ExceptionAnalyzer visitor) {

		int maxKeySize = 0;
		this.exList = visitor.getExceptionList();
		this.rlList = visitor.getMethodRLAnnotationList();

		for (RLMessage msg : exList) {
			if (msg.getKeySize() > maxKeySize) {
				maxKeySize = msg.getKeySize();
			}
		}

		// Throw Exception List
		List<Integer> throwList = new ArrayList<Integer>();
		// Catch Exception List
		List<Integer> catchList = new ArrayList<Integer>();

		if (DEBUG) {
			ConsoleLog.debug("[check]max key size=" + maxKeySize);
		}

		// parse exception call chain
		for (int idx = maxKeySize; idx > 0; idx--) {

			String lastPreKey = "";
			// current try statement deep level
			int lastTryLevel = 0;
			boolean isFirst = true;
			RLMessage msg = null;

			if (exList != null) {
				for (int i = 0, size = exList.size(); i < size; i++) {
					msg = exList.get(i);

					if (msg.getKeySize() == idx) {

						String preKey = idx >= 2 ? msg.getKeyString(idx - 2) : "ROOT";
						String levelKey = msg.getKeyList().get(idx - 1);
						String[] keyItems = new StrTokenizer(levelKey, ".").getTokenArray();
						int tryLevel = Integer.parseInt(keyItems[0]);

						if (DEBUG) {
							ConsoleLog.debug(idx + ":keySize=" + msg.getKeySize() + ":preKey=" + preKey + ":tryLevel="
									+ tryLevel + "=>" + msg);
						}

						if (!isFirst && (!lastPreKey.equals(preKey) || lastTryLevel != tryLevel)) {
							this.checkHandling(throwList, catchList);
							throwList.clear();
							catchList.clear();
						}

						lastTryLevel = tryLevel;
						lastPreKey = preKey;
						isFirst = false;

						if (msg.getRLData().getLevel() >= 0) {
							throwList.add(i);
						}
						else {
							catchList.add(i);
						}
					}
				}
			}
			if (throwList.size() > 0) {
				this.checkHandling(throwList, catchList);
				throwList.clear();
				catchList.clear();
			}
			if (DEBUG) {
				ConsoleLog.debug(idx + " END \n\n");
			}
		}
		this.checkRLHandling();

		return this.exList;
	}

	private void checkRLHandling() {
		if (this.rlList != null) {
			for (int idx = 0, idxsize = this.rlList.size(); idx < idxsize; idx++) {
				RLMessage catchMsg = this.rlList.get(idx);

				if (this.exList != null) {
					for (int i = 0, size = this.exList.size(); i < size; i++) {
						RLMessage throwMsg = this.exList.get(i);
						if (throwMsg.getRLData().getLevel() >= 0 && !throwMsg.isHandleByCatch()) {

							if (throwMsg.equalClassType(catchMsg.getRLData().getExceptionType())) {

								throwMsg.setHandling(true);
								throwMsg
										.setReduction(catchMsg.getRLData().getLevel() < throwMsg.getRLData().getLevel());
								this.exList.set(i, throwMsg);
								catchMsg.addHandleExMap(String.valueOf(i));
							}
						}
					}
				}
				else {
					break;
				}
			}
		}
	}

	private void checkHandling(List<Integer> throwList, List<Integer> catchList) {
		if (DEBUG) {
			ConsoleLog.debug("[checkHandling]START--> throwList size=" + throwList.size() + " catchList size="
					+ catchList.size());
		}

		try {
			// check whether the catch block handle the thrown exception
			for (Integer catchIdx : catchList) {
				RLMessage catchMsg = this.exList.get(catchIdx);

				if (DEBUG) {
					ConsoleLog.debug("[checkHandling] catchMsg=" + catchMsg);
				}

				for (Integer throwIdx : throwList) {
					RLMessage throwMsg = this.exList.get(throwIdx);
					if (DEBUG) {
						ConsoleLog.debug("[checkHandling] throwMsg=" + throwMsg);
					}
					if (!throwMsg.isHandling()) {
						if (throwMsg.getPosition() < catchMsg.getPosition()
								&& throwMsg.equalClassType(catchMsg.getRLData().getExceptionType())) {
							throwMsg.setHandling(true);
							throwMsg.setHandleByCatch(true);
							this.exList.set(throwIdx, throwMsg);
						}
					}
				}
			}

			for (Integer throwIdx : throwList) {
				RLMessage throwMsg = this.exList.get(throwIdx);
				if (!throwMsg.isHandling()) {
					throwMsg.decreaseKeyList();
				}
				if (DEBUG) {
					ConsoleLog.debug("[checkHandling] FINAL throwMsg=" + throwMsg);
				}
			}
		}
		catch (Exception ex) {
			logger.error("[checkHandling] EXCEPTION ",ex);
		}

		if (DEBUG) {
			ConsoleLog.debug("[checkHandling]<---END\n\n");
		}
	}
}
