import { Router } from 'express';
import { 
    getInventory, 
    addInventoryItem, 
    updateInventoryItem, 
    deleteInventoryItem 
} from '../controllers/inventoryController';
import { authenticate, requirePharmacy } from '../middleware/authMiddleware';

const router = Router();

// Protect all inventory routes
router.use(authenticate, requirePharmacy);

router.get('/:pharmacyId', getInventory);
router.post('/:pharmacyId', addInventoryItem);
router.put('/:pharmacyId/:medicineId', updateInventoryItem);
router.delete('/:pharmacyId/:medicineId', deleteInventoryItem);

export default router;