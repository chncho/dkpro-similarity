package de.tudarmstadt.ukp.similarity.dkpro.resource.structure;

import java.util.Map;

import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceSpecifier;

import de.tudarmstadt.ukp.similarity.dkpro.resource.TextSimilarityResourceBase;
import dkpro.similarity.algorithms.structure.TokenPairOrderingMeasure;


public class TokenPairOrderingResource
	extends TextSimilarityResourceBase
{
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public boolean initialize(ResourceSpecifier aSpecifier, Map aAdditionalParams)
        throws ResourceInitializationException
    {
        if (!super.initialize(aSpecifier, aAdditionalParams)) {
            return false;
        }

        this.mode = TextSimilarityResourceMode.list;
        
        try {
            measure = new TokenPairOrderingMeasure();
        }
        catch (NumberFormatException e) {
            throw new ResourceInitializationException(e);
        }
        
        return true;
    }
}