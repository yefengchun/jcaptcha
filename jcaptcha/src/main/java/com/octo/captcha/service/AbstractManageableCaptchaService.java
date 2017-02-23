/*
 * JCaptcha, the open source java framework for captcha definition and integration
 * Copyright (c)  2007 jcaptcha.net. All Rights Reserved.
 * See the LICENSE.txt file distributed with this package.
 */

package com.octo.captcha.service;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.collections.FastHashMap;

import com.octo.captcha.Captcha;
import com.octo.captcha.engine.CaptchaEngine;
import com.octo.captcha.service.captchastore.CaptchaStore;

/**
 * This class provides default implementation for the management interface. It uses an HashMap to store the timestamps
 * for garbage collection.
 *
 * @author <a href="mailto:mag@jcaptcha.net">Marc-Antoine Garrigue</a>
 * @version 1.0
 */
public abstract class AbstractManageableCaptchaService
        extends AbstractCaptchaService
        implements AbstractManageableCaptchaServiceMBean, CaptchaService {


    private int minGuarantedStorageDelayInSeconds;
    private int captchaStoreMaxSize;

    private int captchaStoreSizeBeforeGarbageCollection;

    private int numberOfGeneratedCaptchas = 0;
    private int numberOfCorrectResponse = 0;
    private int numberOfUncorrectResponse = 0;
    private int numberOfGarbageCollectedCaptcha = 0;

    private Map<String, Long> times;

    private long oldestCaptcha = 0;//OPTIMIZATION STUFF!


    @SuppressWarnings("unchecked")
	protected AbstractManageableCaptchaService(CaptchaStore captchaStore, com.octo.captcha.engine.CaptchaEngine captchaEngine,
                                               int minGuarantedStorageDelayInSeconds, int maxCaptchaStoreSize) {
        super(captchaStore, captchaEngine);

        this.setCaptchaStoreMaxSize(maxCaptchaStoreSize);
        this.setMinGuarantedStorageDelayInSeconds(minGuarantedStorageDelayInSeconds);
        this.setCaptchaStoreSizeBeforeGarbageCollection((int) Math.round(0.8 * maxCaptchaStoreSize));
        times = new FastHashMap();
    }

    protected AbstractManageableCaptchaService(CaptchaStore captchaStore, com.octo.captcha.engine.CaptchaEngine captchaEngine,
                                               int minGuarantedStorageDelayInSeconds, int maxCaptchaStoreSize, int captchaStoreLoadBeforeGarbageCollection) {
        this(captchaStore, captchaEngine, minGuarantedStorageDelayInSeconds, maxCaptchaStoreSize);
        if (maxCaptchaStoreSize < captchaStoreLoadBeforeGarbageCollection)
            throw new IllegalArgumentException("the max store size can't be less than garbage collection size. if you want to disable garbage" +
                    " collection (this is not recommended) you may set them equals (max=garbage)");
        this.setCaptchaStoreSizeBeforeGarbageCollection(captchaStoreLoadBeforeGarbageCollection);

    }

    /**
     * Get the fully qualified class name of the concrete CaptchaEngine used by the service.
     *
     * @return the fully qualified class name of the concrete CaptchaEngine used by the service.
     */
    public String getCaptchaEngineClass() {
        return this.engine.getClass().getName();
    }

    /**
     * Set the fully qualified class name of the concrete CaptchaEngine used by the service
     *
     * @param theClassName the fully qualified class name of the CaptchaEngine used by the service
     *
     * @throws IllegalArgumentException if className can't be used as the service CaptchaEngine, either because it can't
     *                                  be instanciated by the service or it is not a ImageCaptchaEngine concrete
     *                                  class.
     */
    public void setCaptchaEngineClass(String theClassName)
            throws IllegalArgumentException {
        try {
            Object engine = Class.forName(theClassName).newInstance();
            if (engine instanceof com.octo.captcha.engine.CaptchaEngine) {
                this.engine = (com.octo.captcha.engine.CaptchaEngine) engine;
            } else {
                throw new IllegalArgumentException("Class is not instance of CaptchaEngine! "
                        + theClassName);
            }
        } catch (InstantiationException e) {
            throw new IllegalArgumentException(e.getMessage());
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException(e.getMessage());
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException(e.getMessage());
        } catch (RuntimeException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    /**
     * @return the engine served by this service
     */
    public CaptchaEngine getEngine() {
        return this.engine;
    }

    /**
     * Updates the engine served by this service
     */
    public void setCaptchaEngine(CaptchaEngine engine) {
        this.engine = engine;
    }

    /**
     * Get the minimum delay (in seconds) a client can be assured that a captcha generated by the service can be
     * retrieved and a response to its challenge tested
     *
     * @return the maximum delay in seconds
     */
    public int getMinGuarantedStorageDelayInSeconds() {
        return minGuarantedStorageDelayInSeconds;
    }

    /**
     * set the minimum delay (in seconds)a client can be assured that a captcha generated by the service can be
     * retrieved and a response to its challenge tested
     *
     * @param theMinGuarantedStorageDelayInSeconds
     *         the minimum guaranted delay
     */
    public void setMinGuarantedStorageDelayInSeconds(int theMinGuarantedStorageDelayInSeconds) {
        this.minGuarantedStorageDelayInSeconds = theMinGuarantedStorageDelayInSeconds;
    }


    /**
     * Get the number of captcha generated since the service is up WARNING : this value won't be significant if the real
     * number is &gt; Long.MAX_VALUE
     *
     * @return the number of captcha generated since the service is up
     */
    public long getNumberOfGeneratedCaptchas() {
        return numberOfGeneratedCaptchas;
    }

    /**
     * Get the number of correct responses to captcha challenges since the service is up. WARNING : this value won't be
     * significant if the real number is &gt; Long.MAX_VALUE
     *
     * @return the number of correct responses since the service is up
     */
    public long getNumberOfCorrectResponses() {
        return numberOfCorrectResponse;
    }

    /**
     * Get the number of uncorrect responses to captcha challenges since the service is up. WARNING : this value won't
     * be significant if the real number is &gt; Long.MAX_VALUE
     *
     * @return the number of uncorrect responses since the service is up
     */
    public long getNumberOfUncorrectResponses() {
        return numberOfUncorrectResponse;
    }

    /**
     * Get the curent size of the captcha store
     *
     * @return the size of the captcha store
     */
    public int getCaptchaStoreSize() {
        return this.store.getSize();
    }

    /**
     * Get the number of captchas that can be garbage collected in the captcha store
     *
     * @return the number of captchas that can be garbage collected in the captcha store
     */
    public int getNumberOfGarbageCollectableCaptchas() {
        return getGarbageCollectableCaptchaIds(System.currentTimeMillis()).size();
    }


    /**
     * Get the number of captcha garbage collected since the service is up WARNING : this value won't be significant if
     * the real number is &gt; Long.MAX_VALUE
     *
     * @return the number of captcha garbage collected since the service is up
     */
    public long getNumberOfGarbageCollectedCaptcha() {
        return numberOfGarbageCollectedCaptcha;
    }

    /**
     * @return the max captchaStore load before garbage collection of the store
     */
    public int getCaptchaStoreSizeBeforeGarbageCollection() {
        return captchaStoreSizeBeforeGarbageCollection;
    }

    /**
     * max captchaStore size before garbage collection of the store
     */
    public void setCaptchaStoreSizeBeforeGarbageCollection(int captchaStoreSizeBeforeGarbageCollection) {
        if (this.captchaStoreMaxSize <
                captchaStoreSizeBeforeGarbageCollection)
            throw new IllegalArgumentException("the max store size can't be less than garbage collection "
                    + "size. if you want to disable garbage" +
                    " collection (this is not recommended) you may "
                    + "set them equals (max=garbage)");

        this.captchaStoreSizeBeforeGarbageCollection =
                captchaStoreSizeBeforeGarbageCollection;
    }

    /**
     * This max size is used by the service : it will throw a CaptchaServiceException if the store is full when a client
     * ask for a captcha.
     */
    public void setCaptchaStoreMaxSize(int size) {
        if (size < this.captchaStoreSizeBeforeGarbageCollection)
            throw new IllegalArgumentException("the max store size can't "
                    + "be less than garbage collection size. if you want "
                    + "to disable garbage" +
                    " collection (this is not recommended) you may "
                    + "set them equals (max=garbage)");
        this.captchaStoreMaxSize = size;
    }

    /**
     * @return the desired max size of the captcha store
     */
    public int getCaptchaStoreMaxSize() {
        return this.captchaStoreMaxSize;
    }

    /**
     * Garbage collect the captcha store, means all old captcha (captcha in the store wich has been stored more than the
     * MinGuarantedStorageDelayInSecond
     * @param garbageCollectableCaptchaIds garbageCollectableCaptchaIds
     */
    protected void garbageCollectCaptchaStore(Iterator<String> garbageCollectableCaptchaIds) {
        // this may cause a captcha disparition if a new captcha is asked between
        // this call and the effective removing from the store!
        long now = System.currentTimeMillis();
        long limit = now - 1000 * minGuarantedStorageDelayInSeconds;

        while (garbageCollectableCaptchaIds.hasNext()) {
            String id = garbageCollectableCaptchaIds.next().toString();
            if (((Long) times.get(id)).longValue() < limit) {
                //remove from times
                times.remove(id);
                //remove from ids
                store.removeCaptcha(id);
                //update stats
                this.numberOfGarbageCollectedCaptcha++;
            }
        }
    }

    public void garbageCollectCaptchaStore() {
        long now = System.currentTimeMillis();
        Collection<String> garbageCollectableCaptchaIds = getGarbageCollectableCaptchaIds(now);
        this.garbageCollectCaptchaStore(garbageCollectableCaptchaIds.iterator());
    }


    /**
     * Empty the Store
     */
    @SuppressWarnings("unchecked")
	public void emptyCaptchaStore() {
        //empty the store
        this.store.empty();
        //And the timestamps
        this.times = new FastHashMap();
    }


    private Collection<String> getGarbageCollectableCaptchaIds(long now) {

        //construct a new collection in order to avoid iterations synchronization pbs :
        // this may cause a captcha disparition if a new captcha is asked between
        // this call and the effective removing from the store!
        HashSet<String> garbageCollectableCaptchas = new HashSet<String>();

        //the time limit under which captchas are collectable
        long limit = now - 1000 * getMinGuarantedStorageDelayInSeconds();
        if (limit > oldestCaptcha) {
            // iterate to find out if the captcha is perimed
			Iterator<String> ids = new HashSet<String>(times.keySet()).iterator();
            while (ids.hasNext()) {
                String id =  ids.next();
                long captchaDate = times.get(id);
                oldestCaptcha = Math.min(captchaDate, oldestCaptcha == 0 ? captchaDate : oldestCaptcha);
                if (captchaDate < limit) {
                    garbageCollectableCaptchas.add(id);
                }
            }
        }
        return garbageCollectableCaptchas;
    }

    //*******
    ///Overriding business methods to add some stats and store management hooks
    ///****

    protected Captcha generateAndStoreCaptcha(Locale locale, String ID) {
        
        //if the store is full try to garbage collect
        if (isCaptchaStoreFull()) {
            //see if possible
            long now = System.currentTimeMillis();
            Collection<String> garbageCollectableCaptchaIds = getGarbageCollectableCaptchaIds(now);
            if (!garbageCollectableCaptchaIds.isEmpty()) {
                //possible collect an rerun
                garbageCollectCaptchaStore(garbageCollectableCaptchaIds.iterator());
                return this.generateAndStoreCaptcha(locale, ID);
            } else {
                //impossible ! has to wait
                throw new CaptchaServiceException("Store is full, try to increase CaptchaStore Size or" +
                        "to decrease time out, or to decrease CaptchaStoreSizeBeforeGrbageCollection");
            }
        }

        if (isCaptchaStoreQuotaReached()) {
            //then garbage collect
            garbageCollectCaptchaStore();
        }
        return generateCountTimeStampAndStoreCaptcha(ID, locale);
    }

    private Captcha generateCountTimeStampAndStoreCaptcha(String ID, Locale locale) {
        //update stats
        numberOfGeneratedCaptchas++;
        //mark as now
        Long now = new Long(System.currentTimeMillis());
        //store in my timestampeds ids
        this.times.put(ID, now);
        //retrieve and store cpatcha
        Captcha captcha = super.generateAndStoreCaptcha(locale, ID);
        return captcha;
    }


    protected boolean isCaptchaStoreFull() {
        return getCaptchaStoreMaxSize() == 0 ? false : getCaptchaStoreSize() >= getCaptchaStoreMaxSize();
    }

    protected boolean isCaptchaStoreQuotaReached() {
        return getCaptchaStoreSize() >= getCaptchaStoreSizeBeforeGarbageCollection();
    }

    /**
     * Method to validate a response to the challenge corresponding to the given ticket and remove the coresponding
     * captcha from the store.
     *
     * @param ID the ticket provided by the buildCaptchaAndGetID method
     *
     * @return true if the response is correct, false otherwise.
     *
     * @throws CaptchaServiceException if the ticket is invalid
     */
    public Boolean validateResponseForID(String ID, Object response) throws CaptchaServiceException {

        Boolean valid = super.validateResponseForID(ID, response);
        //remove from local after because validate may throw an exception if id is not found
        this.times.remove(ID);
        //update stats
        if (valid.booleanValue()) {
            numberOfCorrectResponse++;
        } else {
            numberOfUncorrectResponse++;
        }
        return valid;
    }


}
