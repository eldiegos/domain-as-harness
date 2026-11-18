import { Customer } from "./customer.js";
import { CustomerRepository } from "./customerRepository.js";

export class RegistrationService {
  constructor(private readonly repo: CustomerRepository) {}

  register(name: string, email: string): number {
    const customer = new Customer();
    customer.name = name;
    customer.email = email;
    if (!customer.isValid()) {
      throw new Error("invalid customer");
    }
    return this.repo.save(customer);
  }
}
