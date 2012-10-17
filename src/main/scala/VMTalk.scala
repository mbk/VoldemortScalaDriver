/*
 * Cloud backed storage
 * 
 * Copyright (c) 2010-2012, vrijheid.net
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * *    Redistributions of source code must retain the above copyright
 *      notice, this list of conditions and the following disclaimer.
 * *    Redistributions in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 * *    Neither the name of vrijheid.net nor the
 *      names of its contributors may be used to endorse or promote products
 *      derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
import scala.collection.{JavaConversions => JC}
import java.util.{List => JList,Vector,Iterator,Date}
import voldemort.client._
import voldemort.store.{InsufficientOperationalNodesException}
import voldemort.versioning._
import scala.collection.JavaConversions._

package net.vrijheid.vmtalk { 
	
	class VoldemortWrapperException extends Exception

	class UpdateFailedException extends Exception

	
	trait StoreDelta[K,V] extends StoreClient[K,V]   {
		
		def applyDelta[D](key: K,delta: D, newValue: (V,D) => V)
		def init(key: K,value: V) : Versioned[V]
	}
	

	class VMClient[K,V](delegate: StoreClient[K,V]) extends StoreDelta[K,V]  {
		
		//Set this to...whatever works for you
		private val maxtries =200;
		
		private val do_debug = false
		//Swap in a real logging system if you wish
		private def debug(msg: String) {Console println(msg)}

		//FIX_CC Add method for dispatching failures after max_tries to a log/recovery system by
		//send newValue  and the key there. This can only be done when we're near the end, as it will
		//involve e.g. function serialization (maybe even closures). Hard and subtle stuff with #cases depending
		//On the rest of the code.		
		override def applyDelta[D](key: K,delta: D,newValue: (V,D) => V) {
			debug("applying delta")
			//nifty code here for trying to apply a delta maxtries times
			var tried = 0
			var updated = false
			var next_update: Versioned[V] = get_?(key) match {
				case Some(v) => v
				case None => {throw new VoldemortWrapperException}
			}
			//Guard against null values from Voldemort
			if (! (null == next_update)) {
				next_update setObject newValue(next_update getValue,delta);
				debug("update delta(versioned): " + next_update.toString)
			
				//We are goint to try maxtries until updated
				while (!((tried > maxtries) || (updated))) {
					tried += 1
					try {
						//This wil throw an exception if our data is stale
						put(key,next_update)
						updated = true
						debug("delta applied")
					}
					catch {
						//Stale data, let's try and reconcile
						case o : ObsoleteVersionException => {
							debug("ObsoleteVersionException, retry")
							get_?(key) match {
								case Some(v) => {
									v setObject(newValue(v getValue,delta))
									next_update = v
								}
								case None => {throw new VoldemortWrapperException}
							}
						}
						
						//This is needed for a bug in Voldemort 0.81, see
						//http://groups.google.com/group/project-voldemort/browse_thread/thread/6073a4e362720a42
						//TBD: We should be able to remove this once we upgrade (actually: we MUST remove it, probably)
						//NOTE: we have upgraded, but leave it here for backwrds compatibility
						case p: InsufficientOperationalNodesException => {
							get_?(key) match {
								case Some(v) => {
									v setObject(newValue(v getValue,delta))
									next_update = v
								}
								case None => {throw new VoldemortWrapperException}
							}							
						}
					}
				}
			}
			//This will also be thrown if the key didn't exist 
			if (! updated) {throw new UpdateFailedException}
			debug("applied delta to Voldemort store")
		}
		
		override def applyUpdate(action: UpdateAction[K,V]) = { delegate applyUpdate(action) }
	 	override def applyUpdate(action: UpdateAction[K,V],maxTries: Int) = { delegate applyUpdate(action,maxTries) }
	 	override def delete(key: K) = {delegate delete key}
	 	override def delete(key: K,version: Version) = {delegate delete(key,version)}
	 	
		//Wrap in a Scala Option[Versioned[V]] Monad
		def get_?(key: K): Option[Versioned[V]] = {
			val res = delegate get key
			res match {
				case v: Versioned[_] => Some(v)
				case _ => None
			}
		}
	 	override def get(key: K): Versioned[V] = {delegate get(key)}
		override def get(key: K,defaultValue: Versioned[V]) = {delegate get(key,defaultValue)}
		override def get(key: K, transforms: java.lang.Object ) = {delegate get(key,transforms)}
	 	override def getAll(keys: java.lang.Iterable[K]) = {delegate getAll keys}
	 	override def getAll(keys: java.lang.Iterable[K],transforms: java.util.Map[K,java.lang.Object]) = {delegate getAll(keys,transforms)}
	 	override def getResponsibleNodes(key: K)  = {delegate getResponsibleNodes key}
	    
		//And wrapped in a Scala Option Monad
		def getValue_?(key: K): Option[V] = {
			val res = get_? (key)
			res match {
				case v: Option[Versioned[V]] => v.flatMap((x: Versioned[V]) => {Some(x.getValue())})
				case _ => None
			}
		}
	 	override def getValue(key: K): V = {delegate getValue (key)}
	 	override def getValue(key: K,defaultValue: V): V = {delegate getValue (key,defaultValue)}
	
		override def init(key: K,value: V): Versioned[V] = {
			var versioned = new Versioned(value)

			get_?(key) match {
				//Already exists, so init is redundant
				case Some(v) => {
					debug("key exists, fetched. Done.")
					//But we can always try and update. Effectively-re-init
					//val updater = (value: V,delta: V) => {
					//	delta
					//} 
					//applyDelta(key,value,updater)
					//get(key)
				}
				//Good, we prevent empty keys, initialize
				case _ => {
					debug("key does not exists, creating with default value")
					try {put(key,versioned)} 
					catch { 
						case e => {
							versioned = get_?(key) match {
								case Some(v) => v
								case None => {throw new VoldemortWrapperException}
							}
						}
					}
				}
			}

			debug("In VMClient.init: " + versioned toString)
			versioned
		}
		
	 	override def put(key: K,value: V) = {delegate put(key,value)}
	 	override def put(key: K,versioned: Versioned[V]) = {delegate put(key,versioned)}
	 	override def putIfNotObsolete(key: K,versioned: Versioned[V]) = {delegate putIfNotObsolete(key,versioned)}
		override def put(key: K,  value: V, transforms: java.lang.Object ) = {delegate put(key,value,transforms)}
				
	}
}
