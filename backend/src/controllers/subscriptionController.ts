import { Request, Response } from 'express';
import axios from 'axios';
import { AuthRequest } from '../middleware/authMiddleware';

const REVENUECAT_SECRET_KEY = process.env.REVENUECAT_SECRET_KEY;

/**
 * Performs a server-to-server RevenueCat lookup to determine whether the given
 * user currently has an active premium entitlement.
 */
export const checkPremiumStatus = async (userId: number): Promise<boolean> => {
    if (!REVENUECAT_SECRET_KEY) {
        console.warn('REVENUECAT_SECRET_KEY is missing. Defaulting to free tier.');
        return false;
    }

    try {
        // App User ID in RevenueCat will map to our PostgreSQL user ID
        const response = await axios.get(`https://api.revenuecat.com/v1/subscribers/${userId}`, {
            headers: {
                'Authorization': `Bearer ${REVENUECAT_SECRET_KEY}`,
                'Content-Type': 'application/json'
            }
        });

        const entitlements = response.data?.subscriber?.entitlements;
        
        // Check if the 'premium' entitlement exists and hasn't expired
        if (entitlements && entitlements.premium) {
            const expiresDate = new Date(entitlements.premium.expires_date);
            return expiresDate > new Date(); // True if not expired
        }

        return false;
    } catch (error) {
        console.error('RevenueCat API error:', error);
        return false;
    }
};

/**
 * Returns the authenticated customer's verified subscription state so client
 * apps can gate premium-only benefits without trusting local purchase state.
 */
export const getMySubscriptionStatus = async (req: AuthRequest, res: Response): Promise<void> => {
    try {
        const userId = req.user?.id;
        if (!userId) {
            res.status(401).json({ status: 'error', message: 'Unauthorized' });
            return;
        }

        const isPremium = await checkPremiumStatus(userId);

        res.status(200).json({
            status: 'success',
            data: {
                isPremium,
                benefit: isPremium ? '50% off delivery fees' : 'None'
            }
        });
    } catch (error) {
        res.status(500).json({ status: 'error', message: 'Failed to check subscription status' });
    }
};