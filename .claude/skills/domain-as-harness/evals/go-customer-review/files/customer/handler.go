package customer

import (
	"encoding/json"
	"net/http"
	"strconv"
)

type Handler struct {
	svc  *Service
	repo *Repo
}

func NewHandler(svc *Service, repo *Repo) *Handler {
	return &Handler{svc: svc, repo: repo}
}

// POST /customers
func (h *Handler) Create(w http.ResponseWriter, r *http.Request) {
	var c Customer
	if err := json.NewDecoder(r.Body).Decode(&c); err != nil {
		http.Error(w, err.Error(), http.StatusBadRequest)
		return
	}
	id, err := h.svc.Register(r.Context(), &c)
	if err != nil {
		http.Error(w, err.Error(), http.StatusUnprocessableEntity)
		return
	}
	w.Header().Set("Location", "/customers/"+strconv.FormatInt(id, 10))
	w.WriteHeader(http.StatusCreated)
}

// PUT /customers/{id}
func (h *Handler) Update(w http.ResponseWriter, r *http.Request) {
	id, err := strconv.ParseInt(r.PathValue("id"), 10, 64)
	if err != nil {
		http.Error(w, "bad id", http.StatusBadRequest)
		return
	}
	var c Customer
	if err := json.NewDecoder(r.Body).Decode(&c); err != nil {
		http.Error(w, err.Error(), http.StatusBadRequest)
		return
	}
	c.ID = id
	if err := h.repo.Update(r.Context(), &c); err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}
	w.WriteHeader(http.StatusNoContent)
}
