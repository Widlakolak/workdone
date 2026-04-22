package com.workdone.backend.model;

/**
 * Statusy ofert w cyklu życia mojej aplikacji. 
 * Pomagają mi ogarnąć na jakim etapie jest każda z nich.
 */
public enum OfferStatus {
    NEW,
    ANALYZED,
    APPLIED,
    REJECTED,
    GHOST_JOB,
    ARCHIVED
}