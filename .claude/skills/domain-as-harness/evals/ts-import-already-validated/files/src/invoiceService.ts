import { Customer } from "./customer.js";

export class InvoiceService {
  sendInvoice(customer: Customer): string {
    return `Invoice for ${customer.name} sent to ${customer.email}`;
  }
}
