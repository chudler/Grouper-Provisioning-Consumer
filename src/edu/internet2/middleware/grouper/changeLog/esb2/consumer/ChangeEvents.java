package edu.internet2.middleware.grouper.changeLog.esb2.consumer;

import edu.internet2.middleware.grouper.changeLog.provisioning.ChangeEvent;


/**
 * container around esb event
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  
 * @author mchyzer
 */
public class ChangeEvents {
  
  /** */
    private ChangeEvent[] esbEvent;

	/**
     *
     * @return event array
     */
    public ChangeEvent[] getEsbEvent() {
		return esbEvent;
	}

	/**
	 * 
	 * @param esbEvent
	 */
	public void setEsbEvent(ChangeEvent[] esbEvent) {
		this.esbEvent = esbEvent;
	}

	/**
	 * 
	 * @param esbEvent
	 */
	public void addEsbEvent(ChangeEvent esbEvent) {
		if(this.esbEvent ==null) {
			this.esbEvent = new ChangeEvent[] {esbEvent};
		} else {
            ChangeEvent[] newArray = new ChangeEvent[this.esbEvent.length + 1];
		      System.arraycopy(this.esbEvent, 0, newArray, 0,
		          this.esbEvent.length);
		      newArray[this.esbEvent.length + 1] = esbEvent;
		      this.esbEvent= newArray;
		}
	}
}
