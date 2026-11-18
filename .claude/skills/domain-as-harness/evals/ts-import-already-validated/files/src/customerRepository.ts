import { Customer } from "./customer.js";

export class CustomerRepository {
  private readonly rows = new Map<number, Customer>();
  private next = 1;

  save(customer: Customer): number {
    const id = this.next++;
    customer.id = id;
    this.rows.set(id, customer);
    return id;
  }

  findById(id: number): Customer | undefined {
    return this.rows.get(id);
  }

  update(id: number, customer: Customer): void {
    if (!this.rows.has(id)) throw new Error(`customer ${id} not found`);
    this.rows.set(id, customer);
  }

  findAll(): Customer[] {
    return [...this.rows.values()];
  }
}
