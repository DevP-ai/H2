// Security rules tests for H2 (run with: npm test). Mirrors the spec's Phase 7 checklist.
import { readFileSync } from 'node:fs';
import { after, before, beforeEach, describe, test } from 'node:test';
import { assertFails, assertSucceeds, initializeTestEnvironment } from '@firebase/rules-unit-testing';
import {
  Timestamp, collection, deleteDoc, doc, getDoc, getDocs, query, serverTimestamp, setDoc, updateDoc, where, writeBatch
} from 'firebase/firestore';

let env;

before(async () => {
  env = await initializeTestEnvironment({
    projectId: 'demo-h2',
    firestore: { rules: readFileSync('../firestore.rules', 'utf8'), host: '127.0.0.1', port: 8085 }
  });
});

after(async () => env?.cleanup());

const now = Timestamp.now();
const user = (name, role, cartId) => ({ name, email: `${name}@test.com`, phone: '', role, cartId, createdAt: now, updatedAt: now });
const cart = (name, managerId, status = 'active') => ({ name, location: 'Sector 29', managerId, status, createdAt: now, updatedAt: now });

// admin; managerA -> cart1; managerB -> cart2; managerC -> cart3 (inactive); newbie has no cart
beforeEach(async () => {
  await env.clearFirestore();
  await env.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await setDoc(doc(db, 'users/admin'), user('admin', 'admin', null));
    await setDoc(doc(db, 'users/managerA'), user('managerA', 'manager', 'cart1'));
    await setDoc(doc(db, 'users/managerB'), user('managerB', 'manager', 'cart2'));
    await setDoc(doc(db, 'users/managerC'), user('managerC', 'manager', 'cart3'));
    await setDoc(doc(db, 'users/newbie'), user('newbie', 'manager', null));
    await setDoc(doc(db, 'carts/cart1'), cart('Burger Cart', 'managerA'));
    await setDoc(doc(db, 'carts/cart2'), cart('Momo Cart', 'managerB'));
    await setDoc(doc(db, 'carts/cart3'), cart('Roll Cart', 'managerC', 'inactive'));
    await setDoc(doc(db, 'sales/sale1'), { cartId: 'cart1', amount: 15000, date: now, description: 'Daily sales', createdBy: 'managerA', createdAt: now, updatedAt: now });
    await setDoc(doc(db, 'sales/sale2'), { cartId: 'cart2', amount: 9000, date: now, description: 'Daily sales', createdBy: 'managerB', createdAt: now, updatedAt: now });
    await setDoc(doc(db, 'expenses/exp1'), { cartId: 'cart1', category: 'Gas', amount: 800, date: now, description: '', createdBy: 'managerA', createdAt: now, updatedAt: now });
  });
});

const as = (uid) => env.authenticatedContext(uid, { email: `${uid}@test.com` }).firestore();
const sale = (cartId, createdBy, extra = {}) => ({
  cartId, amount: 1200, date: now, description: 'Lunch sales', createdBy,
  createdAt: serverTimestamp(), updatedAt: serverTimestamp(), ...extra
});
const expense = (cartId, createdBy, extra = {}) => ({ ...sale(cartId, createdBy), category: 'Raw Materials', ...extra });

describe('registration', () => {
  const profile = (uid, extra = {}) => ({
    name: 'New Person', email: `${uid}@test.com`, phone: '', role: 'manager', cartId: null,
    createdAt: serverTimestamp(), updatedAt: serverTimestamp(), ...extra
  });

  test('a new user creates their own manager profile without a cart', async () => {
    await assertSucceeds(setDoc(doc(as('fresh'), 'users/fresh'), profile('fresh')));
  });
  test('a new user cannot register as admin', async () => {
    await assertFails(setDoc(doc(as('fresh'), 'users/fresh'), profile('fresh', { role: 'admin' })));
  });
  test('a new user cannot give themselves a cart', async () => {
    await assertFails(setDoc(doc(as('fresh'), 'users/fresh'), profile('fresh', { cartId: 'cart1' })));
  });
  test('nobody creates a profile for someone else', async () => {
    await assertFails(setDoc(doc(as('fresh'), 'users/other'), profile('other')));
  });
  test('signed-out users cannot read anything', async () => {
    const db = env.unauthenticatedContext().firestore();
    await assertFails(getDoc(doc(db, 'carts/cart1')));
    await assertFails(getDoc(doc(db, 'sales/sale1')));
  });
});

describe('manager → own cart', () => {
  test('reads own cart', async () => {
    await assertSucceeds(getDoc(doc(as('managerA'), 'carts/cart1')));
  });
  test('reads own sales and expenses', async () => {
    const db = as('managerA');
    await assertSucceeds(getDocs(query(collection(db, 'sales'), where('cartId', '==', 'cart1'))));
    await assertSucceeds(getDocs(query(collection(db, 'expenses'), where('cartId', '==', 'cart1'))));
    await assertSucceeds(getDoc(doc(db, 'sales/sale1')));
  });
  test('adds revenue and expenses for own cart', async () => {
    const db = as('managerA');
    await assertSucceeds(setDoc(doc(db, 'sales/newSale'), sale('cart1', 'managerA')));
    await assertSucceeds(setDoc(doc(db, 'expenses/newExp'), expense('cart1', 'managerA')));
  });
  test('edits own name and phone', async () => {
    await assertSucceeds(updateDoc(doc(as('managerA'), 'users/managerA'), { name: 'A', phone: '9876543210', updatedAt: serverTimestamp() }));
  });
});

describe('manager → another cart ❌', () => {
  test('cannot read another cart', async () => {
    await assertFails(getDoc(doc(as('managerA'), 'carts/cart2')));
  });
  test("cannot read another cart's sales, one by one or by query", async () => {
    const db = as('managerA');
    await assertFails(getDoc(doc(db, 'sales/sale2')));
    await assertFails(getDocs(query(collection(db, 'sales'), where('cartId', '==', 'cart2'))));
  });
  test('cannot list every cart or every sale', async () => {
    const db = as('managerA');
    await assertFails(getDocs(collection(db, 'carts')));
    await assertFails(getDocs(collection(db, 'sales')));
  });
  test('cannot add records to another cart', async () => {
    const db = as('managerA');
    await assertFails(setDoc(doc(db, 'sales/x'), sale('cart2', 'managerA')));
    await assertFails(setDoc(doc(db, 'expenses/x'), expense('cart2', 'managerA')));
  });
  test('cannot edit or delete any record, even its own', async () => {
    const db = as('managerA');
    await assertFails(updateDoc(doc(db, 'sales/sale1'), { amount: 1, updatedAt: serverTimestamp() }));
    await assertFails(deleteDoc(doc(db, 'sales/sale1')));
    await assertFails(updateDoc(doc(db, 'sales/sale2'), { amount: 1, updatedAt: serverTimestamp() }));
  });
  test('cannot read other users', async () => {
    await assertFails(getDoc(doc(as('managerA'), 'users/managerB')));
    await assertFails(getDocs(query(collection(as('managerA'), 'users'), where('role', '==', 'manager'))));
  });
  test('cannot create or edit carts', async () => {
    const db = as('managerA');
    await assertFails(setDoc(doc(db, 'carts/mine'), { ...cart('Mine', 'managerA'), createdAt: serverTimestamp(), updatedAt: serverTimestamp() }));
    await assertFails(updateDoc(doc(db, 'carts/cart1'), { status: 'inactive', updatedAt: serverTimestamp() }));
  });
});

describe('manager → change role / cartId ❌', () => {
  test('cannot make themselves admin', async () => {
    await assertFails(updateDoc(doc(as('managerA'), 'users/managerA'), { role: 'admin', updatedAt: serverTimestamp() }));
  });
  test('cannot change their own cart', async () => {
    await assertFails(updateDoc(doc(as('managerA'), 'users/managerA'), { cartId: 'cart2', updatedAt: serverTimestamp() }));
  });
});

describe('records are validated and audited', () => {
  test('amount must be greater than 0', async () => {
    const db = as('managerA');
    await assertFails(setDoc(doc(db, 'sales/x'), sale('cart1', 'managerA', { amount: 0 })));
    await assertFails(setDoc(doc(db, 'sales/y'), sale('cart1', 'managerA', { amount: -50 })));
  });
  test('createdBy must be the signed-in user', async () => {
    await assertFails(setDoc(doc(as('managerA'), 'sales/x'), sale('cart1', 'managerB')));
  });
  test('createdAt must be the server time', async () => {
    await assertFails(setDoc(doc(as('managerA'), 'sales/x'), sale('cart1', 'managerA', { createdAt: Timestamp.fromDate(new Date(2020, 0, 1)) })));
  });
  test('no extra fields such as an image url', async () => {
    await assertFails(setDoc(doc(as('managerA'), 'expenses/x'), expense('cart1', 'managerA', { receiptUrl: 'http://x' })));
  });
  test('an expense needs a category', async () => {
    const { category, ...noCategory } = expense('cart1', 'managerA');
    await assertFails(setDoc(doc(as('managerA'), 'expenses/x'), noCategory));
  });
  test('nothing can be added to an inactive cart', async () => {
    await assertFails(setDoc(doc(as('managerC'), 'sales/x'), sale('cart3', 'managerC')));
  });
});

describe('manager without a cart', () => {
  test('sees no cart data and cannot add any', async () => {
    const db = as('newbie');
    await assertFails(getDoc(doc(db, 'carts/cart1')));
    await assertFails(getDocs(query(collection(db, 'sales'), where('cartId', '==', 'cart1'))));
    await assertFails(setDoc(doc(db, 'sales/x'), sale('cart1', 'newbie')));
  });
  test('can still read their own profile', async () => {
    await assertSucceeds(getDoc(doc(as('newbie'), 'users/newbie')));
  });
});

describe('admin → all carts ✅', () => {
  test('reads every cart, sale, expense and manager', async () => {
    const db = as('admin');
    await assertSucceeds(getDocs(collection(db, 'carts')));
    await assertSucceeds(getDocs(collection(db, 'sales')));
    await assertSucceeds(getDocs(collection(db, 'expenses')));
    await assertSucceeds(getDocs(query(collection(db, 'users'), where('role', '==', 'manager'))));
  });
  test('creates and edits carts', async () => {
    const db = as('admin');
    await assertSucceeds(setDoc(doc(db, 'carts/cart4'), { ...cart('Tea Cart', null), createdAt: serverTimestamp(), updatedAt: serverTimestamp() }));
    await assertSucceeds(updateDoc(doc(db, 'carts/cart1'), { status: 'inactive', updatedAt: serverTimestamp() }));
  });
  test('assigns a manager to a cart (both documents in one batch, like the app)', async () => {
    const db = as('admin');
    const batch = writeBatch(db);
    batch.update(doc(db, 'carts/cart1'), { managerId: 'newbie', updatedAt: serverTimestamp() });
    batch.update(doc(db, 'users/managerA'), { cartId: null, updatedAt: serverTimestamp() });
    batch.update(doc(db, 'users/newbie'), { cartId: 'cart1', updatedAt: serverTimestamp() });
    await assertSucceeds(batch.commit());
  });
  test('cannot assign a cart that does not exist', async () => {
    await assertFails(updateDoc(doc(as('admin'), 'users/newbie'), { cartId: 'nope', updatedAt: serverTimestamp() }));
  });
  test('cannot change anyone\'s role from the app', async () => {
    await assertFails(updateDoc(doc(as('admin'), 'users/managerA'), { role: 'admin', updatedAt: serverTimestamp() }));
  });
  test('cannot delete carts or records', async () => {
    const db = as('admin');
    await assertFails(deleteDoc(doc(db, 'carts/cart1')));
    await assertFails(deleteDoc(doc(db, 'sales/sale1')));
  });
});
