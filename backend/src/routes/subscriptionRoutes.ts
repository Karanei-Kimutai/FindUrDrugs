import { Router } from 'express';
import { getMySubscriptionStatus } from '../controllers/subscriptionController';
import { authenticate, requireCustomer } from '../middleware/authMiddleware';

const router = Router();

// Only authenticated customers can check their premium status
router.get('/status', authenticate, requireCustomer, getMySubscriptionStatus);

export default router;