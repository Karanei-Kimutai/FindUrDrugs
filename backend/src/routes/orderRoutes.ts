import { Router } from 'express';
import { 
    createOrder, getCustomerOrders, getPharmacyOrders, updateOrderStatus 
} from '../controllers/orderController';
import { authenticate, requireCustomer, requirePharmacy } from '../middleware/authMiddleware';

const router = Router();

// Customer endpoints
router.post('/', authenticate, requireCustomer, createOrder);
router.get('/customer', authenticate, requireCustomer, getCustomerOrders);

// Pharmacy endpoints
router.get('/pharmacy/:pharmacyId', authenticate, requirePharmacy, getPharmacyOrders);
router.patch('/:orderId/status', authenticate, requirePharmacy, updateOrderStatus);

export default router;