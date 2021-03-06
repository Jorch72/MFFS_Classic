package com.builtbroken.mffs.api.modules;

/**
 * A grid ModularForcefieldSystem uses to search for machines with frequencies that can be linked and spread Fortron
 * energy.
 *
 * @author Calclavia
 */
public interface IFortronCost
{
    int getFortronCost(float paramFloat);
}
